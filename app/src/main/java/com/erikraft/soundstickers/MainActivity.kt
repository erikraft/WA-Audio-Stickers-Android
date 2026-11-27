@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.erikraft.soundstickers

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.content.res.ColorStateList
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.text.Normalizer
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private enum class VisualKind { VIDEO, IMAGE }

    private var visualUri: Uri? = null
    private var audioUri: Uri? = null
    private var visualName = ""
    private var audioName = ""
    private var visualDurationMs = 0L
    private var audioDurationMs = 0L
    private var sourceFrameRate: Double? = null
    private var visualKind = VisualKind.VIDEO
    private var exportTransformer: Transformer? = null
    private var previewPlayer: ExoPlayer? = null

    private lateinit var content: LinearLayout
    private lateinit var previewPlayerView: PlayerView
    private lateinit var imagePreview: ImageView
    private lateinit var previewPlaceholder: TextView
    private lateinit var visualFileLabel: TextView
    private lateinit var audioFileLabel: TextView
    private lateinit var startInput: TextInputEditText
    private lateinit var endInput: TextInputEditText
    private lateinit var fpsDropdown: AutoCompleteTextView
    private lateinit var audioModeDropdown: AutoCompleteTextView
    private lateinit var createButton: MaterialButton
    private lateinit var progress: LinearProgressIndicator
    private lateinit var progressLabel: TextView

    private val visualPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) onVisualSelected(uri)
        }

    private val audioPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) onAudioSelected(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BACKGROUND
        window.navigationBarColor = BACKGROUND
        window.decorView.systemUiVisibility = 0

        previewPlayer = ExoPlayer.Builder(this).build()
        cleanupExpiredShares()
        buildNativeInterface()
        updateCreateButton()
    }

    private fun buildNativeInterface() {
        val scroll = ScrollView(this).apply {
            clipToPadding = false
            setBackgroundColor(BACKGROUND)
            isFillViewport = true
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(32))
        }
        scroll.addView(
            content,
            ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(scroll)

        buildHeader()
        buildMediaCard()
        buildAudioCard()
        buildTrimCard()
        buildDeliveryCard()
        addView(spacer(dp(24)))
        addText(
            "PROCESSAMENTO LOCAL",
            10f,
            MUTED,
            bold = true,
            letterSpacing = 0.12f
        )
        addText(
            "Seus arquivos são processados no aparelho. O app só compartilha o MP4 final quando você escolhe o WhatsApp.",
            12f,
            MUTED,
            top = dp(7),
            lineSpacing = 1.15f
        )
    }

    private fun buildHeader() {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val mark = TextView(this).apply {
            text = "♫"
            textSize = 27f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = roundedDrawable(GREEN, dp(16).toFloat())
        }
        header.addView(mark, LinearLayout.LayoutParams(dp(52), dp(52)))

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        brand.addView(label("ErikrafT", 17f, TEXT, bold = true))
        brand.addView(label("SOUND STICKERS", 9f, MUTED, bold = true).apply {
            letterSpacing = 0.16f
            setPadding(0, dp(3), 0, 0)
        })
        header.addView(brand, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val localChip = TextView(this).apply {
            text = "●  LOCAL"
            textSize = 9f
            setTextColor(GREEN_LIGHT)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = roundedStrokeDrawable(Color.TRANSPARENT, STROKE, dp(50).toFloat())
        }
        header.addView(localChip)
        content.addView(header)

        addView(spacer(dp(26)))
        addText("SEU ESTÚDIO", 10f, GREEN_LIGHT, bold = true, letterSpacing = 0.18f)
        addText("Crie clipes\ncom identidade sonora.", 31f, TEXT, bold = true, top = dp(10))
        addText(
            "Edite um trecho curto, escolha a trilha e envie direto para o WhatsApp ou WhatsApp Business.",
            14f,
            MUTED,
            top = dp(10),
            lineSpacing = 1.18f
        )
        addView(spacer(dp(24)))
    }

    private fun buildMediaCard() {
        val card = sectionCard("01", "Mídia visual", "Escolha um vídeo ou uma imagem para compor o clipe.")
        val choose = button("Selecionar vídeo ou imagem", filled = false) {
            visualPicker.launch(arrayOf("video/*", "image/*"))
        }
        card.addView(choose, matchWrap().apply { topMargin = dp(16) })

        val preview = FrameLayout(this).apply {
            background = roundedDrawable(PREVIEW, dp(16).toFloat())
        }
        previewPlayerView = PlayerView(this).apply {
            player = previewPlayer
            useController = true
            controllerAutoShow = false
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        }
        imagePreview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
            contentDescription = "Pré-visualização da imagem"
        }
        previewPlaceholder = label(
            "♫\n\nSua prévia aparece aqui",
            13f,
            MUTED,
            gravity = Gravity.CENTER
        )
        preview.addView(previewPlayerView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        preview.addView(imagePreview, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        preview.addView(previewPlaceholder, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        card.addView(preview, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(220)
        ).apply { topMargin = dp(14) })

        visualFileLabel = label("Nenhum arquivo selecionado", 12f, MUTED, top = dp(10))
        card.addView(visualFileLabel)
    }

    private fun buildAudioCard() {
        val card = sectionCard("02", "Faixa de áudio", "Use o som do vídeo ou adicione uma faixa própria.")
        val choose = button("Selecionar áudio (opcional)", filled = false) {
            audioPicker.launch(arrayOf("audio/*"))
        }
        card.addView(choose, matchWrap().apply { topMargin = dp(14) })
        audioFileLabel = label("Nenhum áudio externo selecionado", 12f, MUTED, top = dp(10))
        card.addView(audioFileLabel)

        addTextTo(card, "COMO USAR O ÁUDIO", 9f, MUTED, bold = true, top = dp(18))
        audioModeDropdown = dropdown(
            listOf("Manter áudio original", "Substituir pelo áudio escolhido", "Misturar os dois áudios")
        )
        card.addView(audioModeDropdown, matchWrap().apply { topMargin = dp(7) })
        audioModeDropdown.setText("Manter áudio original", false)
        audioModeDropdown.setOnItemClickListener { _, _, position, _ ->
            if (position > 0 && audioUri == null) {
                audioModeDropdown.setText("Manter áudio original", false)
                showMessage("Selecione uma faixa de áudio primeiro.")
            }
            updateCreateButton()
        }
    }

    private fun buildTrimCard() {
        val card = sectionCard("03", "Ajuste do clipe", "O trecho final pode ter no máximo 10 segundos.")

        val trimRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val startField = textInput("Início (s)", "0.0")
        startInput = startField.second
        val endField = textInput("Final (s)", "5.0")
        endInput = endField.second

        trimRow.addView(startField.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        trimRow.addView(spacer(dp(10)))
        trimRow.addView(endField.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(trimRow, matchWrap().apply { topMargin = dp(16) })

        addTextTo(card, "FPS MÁXIMO DE SAÍDA", 9f, MUTED, bold = true, top = dp(16))
        fpsDropdown = dropdown(listOf(
            "Automático — recomendado",
            "30 FPS", "25 FPS", "20 FPS", "15 FPS", "12 FPS", "10 FPS", "8 FPS", "6 FPS", "5 FPS"
        ))
        card.addView(fpsDropdown, matchWrap().apply { topMargin = dp(7) })
        fpsDropdown.setText("Automático — recomendado", false)

        addTextTo(
            card,
            "O modo automático usa a taxa anunciada pela faixa de vídeo (limitada a 30 FPS). Imagens estáticas usam 5 FPS.",
            11f,
            MUTED,
            top = dp(9),
            lineSpacing = 1.12f
        )
    }

    private fun buildDeliveryCard() {
        val card = sectionCard("04", "Enviar", "Gere um MP4 com o recorte e a faixa escolhidos.")
        createButton = button("Criar clipe e abrir WhatsApp", filled = true) {
            startExport()
        }.apply { isEnabled = false }
        card.addView(createButton, matchWrap().apply { topMargin = dp(14) })

        progress = LinearProgressIndicator(this).apply {
            isIndeterminate = true
            visibility = View.GONE
            trackColor = STROKE
            setIndicatorColor(GREEN)
        }
        card.addView(progress, matchWrap().apply { topMargin = dp(14) })
        progressLabel = label("", 11f, MUTED, top = dp(8)).apply { visibility = View.GONE }
        card.addView(progressLabel)

        addTextTo(
            card,
            "O envio abre a conversa de compartilhamento do WhatsApp. O arquivo é enviado como vídeo; não é instalado automaticamente como figurinha sonora.",
            11f,
            WARNING,
            top = dp(14),
            lineSpacing = 1.12f
        )
    }

    private fun onVisualSelected(uri: Uri) {
        retainReadPermission(uri)
        val mime = contentResolver.getType(uri)?.lowercase(Locale.ROOT).orEmpty()
        val kind = when {
            mime.startsWith("video/") -> VisualKind.VIDEO
            mime.startsWith("image/") -> VisualKind.IMAGE
            else -> {
                showMessage("Escolha um arquivo de vídeo ou imagem compatível.")
                return
            }
        }
        val size = querySize(uri)
        if (size != null && size > MAX_SOURCE_BYTES) {
            showMessage("O arquivo visual ultrapassa 150 MB.")
            return
        }

        val duration = if (kind == VisualKind.VIDEO) readDurationMs(uri) else IMAGE_DEFAULT_DURATION_MS
        if (kind == VisualKind.VIDEO && (duration == null || duration <= 0L)) {
            showMessage("Não foi possível ler a duração deste vídeo.")
            return
        }

        visualUri = uri
        visualKind = kind
        visualName = queryDisplayName(uri).ifBlank { if (kind == VisualKind.VIDEO) "video" else "imagem" }
        visualDurationMs = duration ?: IMAGE_DEFAULT_DURATION_MS
        sourceFrameRate = if (kind == VisualKind.VIDEO) readVideoFrameRate(uri) else null

        if (kind == VisualKind.VIDEO) {
            imagePreview.visibility = View.GONE
            previewPlaceholder.visibility = View.GONE
            previewPlayerView.visibility = View.VISIBLE
            previewPlayer?.apply {
                stop()
                clearMediaItems()
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = false
            }
        } else {
            previewPlayer?.stop()
            previewPlayerView.visibility = View.GONE
            imagePreview.visibility = View.VISIBLE
            previewPlaceholder.visibility = View.GONE
            imagePreview.setImageURI(uri)
        }

        val seconds = visualDurationMs / 1000.0
        startInput.setText("0.0")
        endInput.setText(String.format(Locale.US, "%.1f", minOf(seconds, 10.0)))
        visualFileLabel.text = visualName + if (kind == VisualKind.VIDEO) {
            " · vídeo · " + formatDuration(visualDurationMs)
        } else {
            " · imagem"
        }
        updateCreateButton()
    }

    private fun onAudioSelected(uri: Uri) {
        retainReadPermission(uri)
        val mime = contentResolver.getType(uri)?.lowercase(Locale.ROOT).orEmpty()
        if (!mime.startsWith("audio/")) {
            showMessage("Escolha um arquivo de áudio compatível.")
            return
        }
        val size = querySize(uri)
        if (size != null && size > MAX_SOURCE_BYTES) {
            showMessage("O arquivo de áudio ultrapassa 150 MB.")
            return
        }
        val duration = readDurationMs(uri)
        if (duration == null || duration <= 0L) {
            showMessage("Não foi possível ler a duração do áudio.")
            return
        }
        audioUri = uri
        audioDurationMs = duration
        audioName = queryDisplayName(uri).ifBlank { "audio" }
        audioFileLabel.text = audioName + " · " + formatDuration(duration)
        if (audioModeDropdown.text.isNullOrBlank() ||
            audioModeDropdown.text.toString().startsWith("Manter")
        ) {
            audioModeDropdown.setText("Substituir pelo áudio escolhido", false)
        }
        updateCreateButton()
    }

    private fun startExport() {
        val visual = visualUri ?: return
        if (exportTransformer != null) return

        val startSeconds = startInput.text?.toString()?.toDoubleOrNull() ?: Double.NaN
        val endSeconds = endInput.text?.toString()?.toDoubleOrNull() ?: Double.NaN
        val durationSeconds = visualDurationMs / 1000.0
        if (!NativeExportRules.isValidClip(startSeconds, endSeconds, durationSeconds)) {
            showMessage("O trecho deve ficar dentro da mídia e ter entre 0,1 e 10 segundos.")
            return
        }
        if (visualKind == VisualKind.IMAGE && startSeconds != 0.0) {
            showMessage("Para uma imagem estática, o início do clipe deve ser 0.")
            return
        }

        val modeIndex = audioModeDropdown.listSelection.takeIf { it >= 0 } ?: run {
            when (audioModeDropdown.text?.toString()) {
                "Substituir pelo áudio escolhido" -> 1
                "Misturar os dois áudios" -> 2
                else -> 0
            }
        }
        val customAudio = audioUri
        val useCustomAudio = customAudio != null && modeIndex != 0
        if (visualKind == VisualKind.IMAGE && !useCustomAudio) {
            showMessage("Para criar um clipe a partir de uma imagem, selecione uma faixa de áudio.")
            return
        }

        val fpsSelection = when (fpsDropdown.text?.toString()) {
            "30 FPS" -> "30"
            "25 FPS" -> "25"
            "20 FPS" -> "20"
            "15 FPS" -> "15"
            "12 FPS" -> "12"
            "10 FPS" -> "10"
            "8 FPS" -> "8"
            "6 FPS" -> "6"
            "5 FPS" -> "5"
            else -> "auto"
        }
        val fps = NativeExportRules.resolveFps(fpsSelection, sourceFrameRate, visualKind == VisualKind.IMAGE)
            ?: run {
                showMessage("Selecione um FPS válido.")
                return
            }

        val clipStartMs = (startSeconds * 1000.0).toLong()
        val clipEndMs = (endSeconds * 1000.0).toLong()
        val clipDurationMs = clipEndMs - clipStartMs

        val visualItemBuilder = MediaItem.Builder().setUri(visual)
        if (visualKind == VisualKind.VIDEO) {
            visualItemBuilder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clipStartMs)
                    .setEndPositionMs(clipEndMs)
                    .build()
            )
        } else {
            visualItemBuilder.setImageDurationMs(clipDurationMs)
        }

        val replaceAudio = useCustomAudio && modeIndex == 1
        val editedVisualBuilder = EditedMediaItem.Builder(visualItemBuilder.build())
            .setFrameRate(fps)
        if (replaceAudio && visualKind == VisualKind.VIDEO) {
            editedVisualBuilder.setRemoveAudio(true)
        }
        val editedVisual = editedVisualBuilder.build()

        val visualSequence = if (
            visualKind == VisualKind.VIDEO && !replaceAudio
        ) {
            EditedMediaItemSequence.withAudioAndVideoFrom(listOf(editedVisual))
        } else {
            EditedMediaItemSequence.withVideoFrom(listOf(editedVisual))
        }

        val composition = try {
            if (useCustomAudio) {
                val audio = customAudio ?: throw IllegalStateException("Faixa de áudio ausente.")
                val audioClipEndMs = minOf(audioDurationMs, clipDurationMs)
                if (audioClipEndMs <= 0L) throw IllegalStateException("A faixa de áudio está vazia.")
                val audioItem = MediaItem.Builder()
                    .setUri(audio)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(0L)
                            .setEndPositionMs(audioClipEndMs)
                            .build()
                    )
                    .build()
                val audioSequence = EditedMediaItemSequence
                    .withAudioFrom(listOf(EditedMediaItem.Builder(audioItem).build()))
                    .buildUpon()
                    .setIsLooping(audioDurationMs < clipDurationMs)
                    .build()
                Composition.Builder(visualSequence, audioSequence).build()
            } else {
                Composition.Builder(visualSequence).build()
            }
        } catch (error: Exception) {
            showMessage(error.message ?: "Não foi possível preparar a composição.")
            return
        }

        stopPreviewAudio()
        previewPlayer?.pause()
        val outputDirectory = getShareDirectory()
        val outputFile = File(
            outputDirectory,
            safeOutputBaseName(visualName) + "-" + System.currentTimeMillis() + ".mp4"
        )
        setExportBusy(true, "Renderizando MP4 a $fps FPS…")

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, result: ExportResult) {
                exportTransformer = null
                setExportBusy(false)
                if (!outputFile.isFile || outputFile.length() <= 0L) {
                    outputFile.delete()
                    showMessage("A exportação terminou sem gerar um arquivo válido.")
                    return
                }
                showMessage("Clipe criado. Escolha o WhatsApp para enviar.")
                openWhatsAppSelector(outputFile)
            }

            override fun onError(
                composition: Composition,
                result: ExportResult,
                exception: ExportException
            ) {
                exportTransformer = null
                setExportBusy(false)
                outputFile.delete()
                showMessage(exception.message ?: "Falha ao renderizar o clipe neste dispositivo.")
            }
        }

        try {
            exportTransformer = Transformer.Builder(this)
                .addListener(listener)
                .build()
            exportTransformer?.start(composition, outputFile.absolutePath)
        } catch (error: Exception) {
            exportTransformer = null
            setExportBusy(false)
            outputFile.delete()
            showMessage(error.message ?: "Não foi possível iniciar a exportação.")
        }
    }

    private fun openWhatsAppSelector(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.shared", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, file.name)
            clipData = ClipData.newUri(contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val targets = listOf(
            "WhatsApp" to "com.whatsapp",
            "WhatsApp Business" to "com.whatsapp.w4b"
        ).filter { (_, packageName) ->
            packageManager.getLaunchIntentForPackage(packageName) != null
        }

        when (targets.size) {
            0 -> {
                showMessage("WhatsApp não encontrado. Abrindo o compartilhamento do Android.")
                openGeneralShareChooser(sendIntent)
            }
            1 -> launchShareTarget(sendIntent, targets.first().second)
            else -> AlertDialog.Builder(this)
                .setTitle("Enviar clipe")
                .setItems(targets.map { it.first }.toTypedArray()) { _, index ->
                    targets.getOrNull(index)?.second?.let { launchShareTarget(sendIntent, it) }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun launchShareTarget(sendIntent: Intent, targetPackage: String) {
        try {
            startActivity(Intent(sendIntent).setPackage(targetPackage))
        } catch (_: ActivityNotFoundException) {
            showMessage("Não foi possível abrir o WhatsApp escolhido.")
            openGeneralShareChooser(sendIntent)
        }
    }

    private fun openGeneralShareChooser(sendIntent: Intent) {
        sendIntent.setPackage(null)
        try {
            startActivity(Intent.createChooser(sendIntent, "Compartilhar clipe"))
        } catch (_: ActivityNotFoundException) {
            showMessage("Nenhum aplicativo compatível foi encontrado.")
        }
    }

    private fun setExportBusy(busy: Boolean, message: String = "") {
        createButton.isEnabled = !busy && visualUri != null &&
            (visualKind == VisualKind.VIDEO || audioUri != null)
        createButton.text = if (busy) "Preparando clipe…" else "Criar clipe e abrir WhatsApp"
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        progressLabel.visibility = if (busy) View.VISIBLE else View.GONE
        progressLabel.text = message
    }

    private fun updateCreateButton() {
        if (!::createButton.isInitialized || exportTransformer != null) return
        val validVisual = visualUri != null
        val validAudio = visualKind == VisualKind.VIDEO || audioUri != null
        createButton.isEnabled = validVisual && validAudio
    }

    private fun retainReadPermission(uri: Uri) {
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun readDurationMs(uri: Uri): Long? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } finally {
            retriever.release()
        }
    }.getOrNull()

    private fun readVideoFrameRate(uri: Uri): Double? = runCatching {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(this, uri, null)
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/") && format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                    val value = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                    if (value > 0) return@runCatching value.toDouble()
                }
            }
            null
        } finally {
            extractor.release()
        }
    }.getOrNull()

    private fun queryDisplayName(uri: Uri): String {
        var result = ""
        val cursor: Cursor? = contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) result = it.getString(index).orEmpty()
            }
        }
        return result
    }

    private fun querySize(uri: Uri): Long? {
        val cursor = contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0) return it.getLong(index).takeIf { size -> size >= 0L }
            }
        }
        return null
    }

    private fun getShareDirectory(): File {
        val directory = File(cacheDir, "shared")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("Não foi possível criar o diretório temporário.")
        }
        return directory
    }

    private fun cleanupExpiredShares() {
        val directory = File(cacheDir, "shared")
        val cutoff = System.currentTimeMillis() - CACHE_TTL_MS
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) file.delete()
        }
    }

    private fun safeOutputBaseName(value: String): String {
        val withoutExtension = value.substringBeforeLast('.', value)
        val normalized = Normalizer.normalize(withoutExtension, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_-]+"), "-")
            .trim('-', '_')
            .take(48)
        return normalized.ifBlank { "erikraft-sound-clip" }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000L
        return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60L, totalSeconds % 60L)
    }

    private fun sectionCard(number: String, title: String, description: String): LinearLayout {
        val card = MaterialCardView(this).apply {
            radius = dp(22).toFloat()
            cardElevation = dp(1).toFloat()
            setCardBackgroundColor(SURFACE)
            strokeWidth = dp(1)
            setStrokeColor(ColorStateList.valueOf(STROKE))
            setContentPadding(dp(18), dp(18), dp(18), dp(18))
        }
        val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val badge = TextView(this).apply {
            text = number
            textSize = 10f
            setTextColor(GREEN_LIGHT)
            gravity = Gravity.CENTER
            background = roundedDrawable(GREEN_TINT, dp(9).toFloat())
        }
        heading.addView(badge, LinearLayout.LayoutParams(dp(34), dp(34)))
        val titleBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), 0, 0, 0)
        }
        titleBlock.addView(label(title, 15f, TEXT, bold = true))
        titleBlock.addView(label(description, 11f, MUTED, top = dp(3)))
        heading.addView(titleBlock, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        inner.addView(heading)
        card.addView(inner)
        content.addView(card, matchWrap().apply {
            topMargin = dp(11)
        })
        return inner
    }

    private fun textInput(hintText: String, initial: String): Pair<TextInputLayout, TextInputEditText> {
        val layout = TextInputLayout(this).apply {
            hint = hintText
            boxBackgroundMode = TextInputLayout.BOX_OUTLINE
            boxStrokeColor = GREEN
            setBoxCornerRadii(dp(12).toFloat(), dp(12).toFloat(), dp(12).toFloat(), dp(12).toFloat())
        }
        val input = TextInputEditText(this).apply {
            setTextColor(TEXT)
            setHintTextColor(MUTED)
            setTextSize(15f)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
            setText(initial)
        }
        layout.addView(input, TextInputLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        return layout to input
    }

    private fun dropdown(options: List<String>): AutoCompleteTextView {
        val field = TextInputLayout(this).apply {
            boxBackgroundMode = TextInputLayout.BOX_OUTLINE
            boxStrokeColor = STROKE
            setBoxCornerRadii(dp(12).toFloat(), dp(12).toFloat(), dp(12).toFloat(), dp(12).toFloat())
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
        }
        val input = AutoCompleteTextView(this).apply {
            setTextColor(TEXT)
            setHintTextColor(MUTED)
            setTextSize(14f)
            inputType = InputType.TYPE_NULL
            keyListener = null
            threshold = 0
            setAdapter(ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_list_item_1,
                options
            ))
            setOnClickListener { showDropDown() }
        }
        field.addView(input, TextInputLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(54)
        ))
        field.tag = input
        return input
    }

    private fun button(text: String, filled: Boolean, action: () -> Unit): MaterialButton {
        return MaterialButton(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 13f
            cornerRadius = dp(14)
            insetTop = 0
            insetBottom = 0
            minHeight = dp(52)
            setOnClickListener { action() }
            backgroundTintList = ColorStateList.valueOf(if (filled) GREEN else SURFACE_HIGH)
            setTextColor(if (filled) DARK_TEXT else GREEN_LIGHT)
            strokeWidth = if (filled) 0 else dp(1)
            strokeColor = ColorStateList.valueOf(STROKE)
        }
    }

    private fun addText(
        text: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        top: Int = 0,
        gravity: Int = Gravity.START,
        letterSpacing: Float = 0f,
        lineSpacing: Float = 1f
    ): TextView {
        val view = label(text, size, color, bold, gravity, top, letterSpacing, lineSpacing)
        content.addView(view)
        return view
    }

    private fun addTextTo(
        parent: LinearLayout,
        text: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        top: Int = 0,
        lineSpacing: Float = 1f
    ): TextView {
        val view = label(text, size, color, bold, Gravity.START, top, lineSpacing = lineSpacing)
        parent.addView(view)
        return view
    }

    private fun label(
        text: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        gravity: Int = Gravity.START,
        top: Int = 0,
        letterSpacing: Float = 0f,
        lineSpacing: Float = 1f
    ): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        this.gravity = gravity
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        this.letterSpacing = letterSpacing
        setLineSpacing(dp(3).toFloat(), lineSpacing)
        if (top > 0) setPadding(0, top, 0, 0)
    }

    private fun roundedDrawable(color: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }

    private fun roundedStrokeDrawable(color: Int, stroke: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            setStroke(dp(1), stroke)
        }

    private fun spacer(height: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, height)
    }

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun stopPreviewAudio() {
        previewPlayer?.pause()
    }

    override fun onDestroy() {
        exportTransformer?.let { transformer ->
            runCatching { transformer.cancel() }
        }
        exportTransformer = null
        previewPlayer?.release()
        previewPlayer = null
        super.onDestroy()
    }

    private companion object {
        const val GREEN = 0xFF25D366.toInt()
        const val GREEN_LIGHT = 0xFF9AF2B9.toInt()
        const val GREEN_TINT = 0x1F25D366
        const val BACKGROUND = 0xFF101019.toInt()
        const val SURFACE = 0xFF1B1C27.toInt()
        const val SURFACE_HIGH = 0xFF252736.toInt()
        const val PREVIEW = 0xFF0B0C12.toInt()
        const val STROKE = 0xFF343646.toInt()
        const val TEXT = 0xFFF4F5F7.toInt()
        const val MUTED = 0xFFA5A9B8.toInt()
        const val WARNING = 0xFFFFD18A.toInt()
        const val DARK_TEXT = 0xFF07180D.toInt()
        const val IMAGE_DEFAULT_DURATION_MS = 5_000L
        const val MAX_SOURCE_BYTES = 150L * 1024L * 1024L
        const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L
    }
}
