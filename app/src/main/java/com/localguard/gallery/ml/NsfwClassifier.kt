package com.localguard.gallery.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import java.nio.FloatBuffer
import kotlin.math.exp

/** Per-class probabilities produced by the on-device model. */
data class NsfwPrediction(
    val drawings: Float,
    val hentai: Float,
    val neutral: Float,
    val porn: Float,
    val sexy: Float,
) {
    /** Probability that the content is explicit (porn or hentai), optionally counting suggestive content. */
    fun score(includeSuggestive: Boolean): Float =
        porn + hentai + if (includeSuggestive) sexy else 0f
}

/**
 * Runs a MobileNetV4 NSFW classifier (ONNX, bundled in assets) fully offline.
 *
 * The model takes a single CHW float tensor [3, 224, 224] with values in 0..1 (ImageNet
 * normalisation is baked into the graph) and returns logits for
 * [drawings, hentai, neutral, porn, sexy].
 */
class NsfwClassifier(context: Context) : AutoCloseable {

    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val inputName: String

    init {
        val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
        val options = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(2)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }
        session = env.createSession(bytes, options)
        inputName = session.inputNames.first()
    }

    /** Thread-safe: ORT sessions support concurrent run() calls. */
    fun classify(source: Bitmap): NsfwPrediction {
        val input = ImagePreprocessor.toChwFloats(source, INPUT_SIZE)
        val shape = longArrayOf(3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
        OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape).use { tensor ->
            session.run(mapOf(inputName to tensor)).use { result ->
                @Suppress("UNCHECKED_CAST")
                val logits = (result[0].value as Array<FloatArray>)[0]
                val p = softmax(logits)
                return NsfwPrediction(p[0], p[1], p[2], p[3], p[4])
            }
        }
    }

    override fun close() {
        session.close()
    }

    companion object {
        const val MODEL_ASSET = "nsfw_mobilenetv4.onnx"
        const val INPUT_SIZE = 224

        fun softmax(logits: FloatArray): FloatArray {
            val max = logits.max()
            val exps = logits.map { exp((it - max).toDouble()) }
            val sum = exps.sum()
            return FloatArray(logits.size) { (exps[it] / sum).toFloat() }
        }
    }
}
