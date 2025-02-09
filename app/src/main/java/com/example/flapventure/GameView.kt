package com.example.flapventure

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.random.Random

// Data class representing a pair of pipes and a flag for scoring.
data class PipePair(var top: Rect, var bottom: Rect, var scored: Boolean = false)

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs),
    Runnable, SurfaceHolder.Callback {

    private var gameThread: Thread? = null
    @Volatile private var isPlaying = false
    private val surfaceHolder: SurfaceHolder = holder
    private val paint = Paint()

    // --- Bird Sprite Setup with Transparency and Scaling ---
    private val bitmapOptions = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    private val originalBirdBitmap: Bitmap =
        BitmapFactory.decodeResource(context.resources, R.drawable.bird, bitmapOptions)
    private val desiredWidth = (70 * 1.8 * resources.displayMetrics.density).toInt()
    private val desiredHeight = (56 * 1.8 * resources.displayMetrics.density).toInt()
    private val birdBitmap: Bitmap =
        Bitmap.createScaledBitmap(originalBirdBitmap, desiredWidth, desiredHeight, true)
    private val birdWidth = birdBitmap.width.toFloat()
    private val birdHeight = birdBitmap.height.toFloat()

    // --- Bird Physics Properties ---
    private var birdX = 100f
    private var birdY = 300f
    private var birdVel = 0f
    private val gravity = 0.5f
    private val jumpStrength = -10f

    // --- Pipe Properties (drawn as blocks) ---
    private val pipeWidth = 150
    private val pipeGap = 600
    private val pipeVelocity = 5f
    private var lastPipeTime = System.currentTimeMillis()
    private val pipeInterval = 2000L
    private val pipes = mutableListOf<PipePair>()

    // --- Game State ---
    private var score = 0
    private var gameOver = false
    private var gameOverSoundPlayed = false

    init {
        surfaceHolder.addCallback(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Center the bird vertically and position it at 25% of the screen width.
        birdX = (w * 0.25).toFloat()
        birdY = (h / 2f - birdHeight / 2f)
    }

    override fun run() {
        while (isPlaying) {
            update()
            draw()
            control()
        }
    }

    private fun update() {
        if (!gameOver) {
            birdVel += gravity
            birdY += birdVel

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPipeTime > pipeInterval) {
                lastPipeTime = currentTime
                addPipe()
            }

            val iterator = pipes.iterator()
            while (iterator.hasNext()) {
                val pipePair = iterator.next()
                pipePair.top.offset((-pipeVelocity).toInt(), 0)
                pipePair.bottom.offset((-pipeVelocity).toInt(), 0)

                if (!pipePair.scored && pipePair.top.right < birdX) {
                    score++
                    pipePair.scored = true
                }
                if (pipePair.top.right < 0) {
                    iterator.remove()
                }
            }

            val insetX = (birdWidth * 0.3).toInt()
            val insetY = (birdHeight * 0.3).toInt()
            val birdRect = Rect(
                (birdX + insetX).toInt(),
                (birdY + insetY).toInt(),
                (birdX + birdWidth - insetX).toInt(),
                (birdY + birdHeight - insetY).toInt()
            )

            for (pipePair in pipes) {
                if (Rect.intersects(birdRect, pipePair.top) ||
                    Rect.intersects(birdRect, pipePair.bottom)) {
                    gameOver = true
                }
            }
            if (birdY < 0 || birdY + birdHeight > height) {
                gameOver = true
            }
        } else {
            if (!gameOverSoundPlayed) {
                playGameOverSound()
            }
        }
    }

    private fun playGameOverSound() {
        val mp = MediaPlayer.create(context, R.raw.gameover)
        mp.start()
        mp.setOnCompletionListener { it.release() }
        gameOverSoundPlayed = true
    }

    private fun addPipe() {
        val minPipeHeight = 100
        val maxPipeHeight = height - pipeGap - 100
        val topPipeHeight = Random.nextInt(minPipeHeight, maxPipeHeight)
        val topRect = Rect(width, 0, width + pipeWidth, topPipeHeight)
        val bottomRect = Rect(width, topPipeHeight + pipeGap, width + pipeWidth, height)
        pipes.add(PipePair(topRect, bottomRect))
    }

    private fun draw() {
        if (surfaceHolder.surface.isValid) {
            val canvas: Canvas = surfaceHolder.lockCanvas()
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(birdBitmap, birdX, birdY, paint)
            paint.color = Color.GREEN
            for (pipePair in pipes) {
                canvas.drawRect(pipePair.top, paint)
                canvas.drawRect(pipePair.bottom, paint)
            }
            paint.color = Color.BLUE
            paint.textSize = 60f
            canvas.drawText("Score: $score", 50f, 100f, paint)
            if (gameOver) {
                paint.textSize = 100f
                canvas.drawText("Game Over", width / 4f, height / 2f, paint)
                paint.textSize = 60f
                canvas.drawText("Tap to restart", width / 3f, height / 2f + 80, paint)
            }
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    private fun control() {
        try {
            Thread.sleep(17)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            performClick()
            if (!gameOver) {
                birdVel = jumpStrength
            } else {
                resetGame()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun resetGame() {
        birdX = (width * 0.25).toFloat()
        birdY = (height / 2f - birdHeight / 2f)
        birdVel = 0f
        pipes.clear()
        score = 0
        gameOver = false
        gameOverSoundPlayed = false
        lastPipeTime = System.currentTimeMillis()
    }

    fun pause() {
        isPlaying = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun resume() {
        isPlaying = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    override fun surfaceCreated(holder: SurfaceHolder) { resume() }
    override fun surfaceDestroyed(holder: SurfaceHolder) { pause() }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { }
}
