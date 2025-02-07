package com.example.flapventure

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
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

    // --- Bird Sprite Setup with Transparency ---
    // Use BitmapFactory options to preserve transparency.
    private val bitmapOptions = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    // Load the bird.webp image from res/drawable.
    private val originalBirdBitmap: Bitmap =
        BitmapFactory.decodeResource(context.resources, R.drawable.bird, bitmapOptions)
    // Calculate desired dimensions in pixels (140dp x 112dp for twice the previous size).
    private val desiredWidth = (140 * resources.displayMetrics.density).toInt()
    private val desiredHeight = (112 * resources.displayMetrics.density).toInt()
    // Scale the image to the desired dimensions.
    private val birdBitmap: Bitmap =
        Bitmap.createScaledBitmap(originalBirdBitmap, desiredWidth, desiredHeight, true)
    // Use these dimensions for drawing.
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
    // Increased pipe gap: now 50% wider, from 400 to 600.
    private val pipeGap = 600
    private val pipeVelocity = 5f
    private var lastPipeTime = System.currentTimeMillis()
    private val pipeInterval = 2000L // milliseconds between pipe generations
    private val pipes = mutableListOf<PipePair>()

    // --- Game State ---
    private var score = 0
    private var gameOver = false

    init {
        surfaceHolder.addCallback(this)
    }

    override fun run() {
        while (isPlaying) {
            update()
            draw()
            control()
        }
    }

    // Update game state.
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

            // Create a smaller collision rectangle for the bird (inset by 10% on all sides).
            val insetX = (birdWidth * 0.1).toInt()
            val insetY = (birdHeight * 0.1).toInt()
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
        }
    }

    // Create a new pipe pair with a random gap.
    private fun addPipe() {
        val minPipeHeight = 100
        val maxPipeHeight = height - pipeGap - 100
        val topPipeHeight = Random.nextInt(minPipeHeight, maxPipeHeight)
        val topRect = Rect(width, 0, width + pipeWidth, topPipeHeight)
        val bottomRect = Rect(width, topPipeHeight + pipeGap, width + pipeWidth, height)
        pipes.add(PipePair(topRect, bottomRect))
    }

    // Draw game elements.
    private fun draw() {
        if (surfaceHolder.surface.isValid) {
            val canvas: Canvas = surfaceHolder.lockCanvas()
            // Clear the canvas with white.
            canvas.drawColor(Color.WHITE)

            // Draw the bird using the bird.webp sprite.
            canvas.drawBitmap(birdBitmap, birdX, birdY, paint)

            // Draw pipes as green rectangles.
            paint.color = Color.GREEN
            for (pipePair in pipes) {
                canvas.drawRect(pipePair.top, paint)
                canvas.drawRect(pipePair.bottom, paint)
            }

            // Draw the score in blue.
            paint.color = Color.BLUE
            paint.textSize = 60f
            canvas.drawText("Score: $score", 50f, 100f, paint)

            // If game over, display game over messages.
            if (gameOver) {
                paint.textSize = 100f
                canvas.drawText("Game Over", width / 4f, height / 2f, paint)
                paint.textSize = 60f
                canvas.drawText("Tap to restart", width / 3f, height / 2f + 80, paint)
            }
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    // Control frame rate (~60fps).
    private fun control() {
        try {
            Thread.sleep(17)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            performClick() // For accessibility.
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

    // Reset the game state.
    private fun resetGame() {
        birdX = 100f
        birdY = 300f
        birdVel = 0f
        pipes.clear()
        score = 0
        gameOver = false
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

    // SurfaceHolder callbacks.
    override fun surfaceCreated(holder: SurfaceHolder) { resume() }
    override fun surfaceDestroyed(holder: SurfaceHolder) { pause() }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { }
}
