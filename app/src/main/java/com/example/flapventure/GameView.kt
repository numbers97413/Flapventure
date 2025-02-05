package com.example.flapventure

import android.content.Context
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

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), Runnable, SurfaceHolder.Callback {

    private var gameThread: Thread? = null
    @Volatile private var isPlaying = false
    private val surfaceHolder: SurfaceHolder = holder
    private val paint = Paint()

    // Bird properties.
    private var birdX = 100f
    private var birdY = 300f
    private val birdWidth = 50
    private val birdHeight = 40
    private var birdVel = 0f
    private val gravity = 0.5f
    private val jumpStrength = -10f

    // Pipe properties.
    private val pipeWidth = 150
    private val pipeGap = 300
    private val pipeVelocity = 5f
    private var lastPipeTime = System.currentTimeMillis()
    private val pipeInterval = 2000L // milliseconds between pipe generations
    private val pipes = mutableListOf<PipePair>()

    // Score and game state.
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
            // Update bird physics.
            birdVel += gravity
            birdY += birdVel

            // Generate new pipes at intervals.
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPipeTime > pipeInterval) {
                lastPipeTime = currentTime
                addPipe()
            }

            // Move pipes and update score.
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

            // Check collisions.
            val birdRect = Rect(birdX.toInt(), birdY.toInt(), (birdX + birdWidth).toInt(), (birdY + birdHeight).toInt())
            for (pipePair in pipes) {
                if (Rect.intersects(birdRect, pipePair.top) ||
                    Rect.intersects(birdRect, pipePair.bottom)) {
                    gameOver = true
                }
            }

            // Check screen boundaries.
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

    // Draw game objects using simple blocks.
    private fun draw() {
        if (surfaceHolder.surface.isValid) {
            val canvas: Canvas = surfaceHolder.lockCanvas()
            // Clear the canvas with a white background.
            canvas.drawColor(Color.WHITE)

            // Draw the bird as a black rectangle.
            paint.color = Color.BLACK
            canvas.drawRect(birdX, birdY, birdX + birdWidth, birdY + birdHeight, paint)

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

            // Display a game over message if needed.
            if (gameOver) {
                paint.textSize = 100f
                canvas.drawText("Game Over", width / 4f, height / 2f, paint)
                paint.textSize = 60f
                canvas.drawText("Tap to restart", width / 3f, height / 2f + 80, paint)
            }
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    // Control the frame rate (~60fps).
    private fun control() {
        try {
            Thread.sleep(17)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            if (!gameOver) {
                birdVel = jumpStrength
            } else {
                resetGame()
            }
        }
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
