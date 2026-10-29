package org.gotson.komga.infrastructure.configuration

import org.gotson.komga.domain.model.Dimension
import org.gotson.komga.infrastructure.image.ImageAnalyzer
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class LogoImageService(
    private val imageAnalyzer: ImageAnalyzer,
    private val komgaProperties: KomgaProperties
) {
    private val LOGO_DIR: Path
    
    init {
        // Use config directory from KomgaProperties
        val configDir = komgaProperties.configDir ?: "config"
        LOGO_DIR = Paths.get(configDir, "logos")
        
        // Create logos directory if it doesn't exist
        if (!Files.exists(LOGO_DIR)) {
            Files.createDirectories(LOGO_DIR)
        }
    }
    
    fun saveLogoImage(imageBytes: ByteArray, fileName: String): String {
        val logoPath = LOGO_DIR.resolve(fileName)
        Files.write(logoPath, imageBytes)
        return fileName
    }
    
    fun getLogoImagePath(fileName: String): Path {
        return LOGO_DIR.resolve(fileName)
    }
    
    fun deleteLogoImage(fileName: String) {
        val logoPath = LOGO_DIR.resolve(fileName)
        if (Files.exists(logoPath)) {
            Files.delete(logoPath)
        }
    }
    
    fun getLogoImage(fileName: String): ByteArray? {
        val logoPath = LOGO_DIR.resolve(fileName)
        return if (Files.exists(logoPath)) {
            Files.readAllBytes(logoPath)
        } else {
            null
        }
    }
    
    fun validateLogoImage(imageBytes: ByteArray): Boolean {
        return try {
            val dimension = imageAnalyzer.getDimension(imageBytes.inputStream()) ?: Dimension(0, 0)
            
            // Validate image dimensions (512x512px)
            dimension.width == 512 && dimension.height == 512
        } catch (e: Exception) {
            false
        }
    }
}