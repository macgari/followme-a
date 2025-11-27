package com.followme.attendance.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.charset.Charset

/**
 * NFC tag reader and writer
 */
class NfcHandler {
    
    private val gson = Gson()
    
    // ========== Reading Tags ==========
    
    fun readTag(tag: Tag): NfcTagData? {
        return try {
            val ndef = Ndef.get(tag) ?: return null
            ndef.connect()
            
            val ndefMessage = ndef.cachedNdefMessage
            if (ndefMessage == null) {
                ndef.close()
                return null
            }
            
            val records = ndefMessage.records
            var jsonData: Map<String, String>? = null
            var uriData: String? = null
            
            for (record in records) {
                when {
                    // JSON record
                    record.toMimeType() == "application/json" -> {
                        val payload = String(record.payload, Charset.forName("UTF-8"))
                        jsonData = parseJsonPayload(payload)
                    }
                    // URI record
                    record.tnf == NdefRecord.TNF_WELL_KNOWN && 
                    record.type.contentEquals(NdefRecord.RTD_URI) -> {
                        uriData = parseUriRecord(record)
                    }
                }
            }
            
            ndef.close()
            
            if (jsonData != null || uriData != null) {
                NfcTagData(
                    data = jsonData ?: emptyMap(),
                    url = uriData,
                    tagId = bytesToHex(tag.id)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun parseJsonPayload(payload: String): Map<String, String>? {
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(payload, type)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseUriRecord(record: NdefRecord): String? {
        return try {
            val payload = record.payload
            if (payload.isEmpty()) return null
            
            val prefixCode = payload[0].toInt() and 0xFF
            val prefix = getUriPrefix(prefixCode)
            val uri = String(payload, 1, payload.size - 1, Charset.forName("UTF-8"))
            
            prefix + uri
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getUriPrefix(code: Int): String {
        return when (code) {
            0x00 -> ""
            0x01 -> "http://www."
            0x02 -> "https://www."
            0x03 -> "http://"
            0x04 -> "https://"
            0x05 -> "tel:"
            0x06 -> "mailto:"
            0x07 -> "ftp://anonymous:anonymous@"
            0x08 -> "ftp://ftp."
            0x09 -> "ftps://"
            0x0A -> "sftp://"
            0x0B -> "smb://"
            0x0C -> "nfs://"
            0x0D -> "ftp://"
            0x0E -> "dav://"
            0x0F -> "news:"
            0x10 -> "telnet://"
            0x11 -> "imap:"
            0x12 -> "rtsp://"
            0x13 -> "urn:"
            0x14 -> "pop:"
            0x15 -> "sip:"
            0x16 -> "sips:"
            0x17 -> "tftp:"
            0x18 -> "btspp://"
            0x19 -> "btl2cap://"
            0x1A -> "btgoep://"
            0x1B -> "tcpobex://"
            0x1C -> "irdaobex://"
            0x1D -> "file://"
            0x1E -> "urn:epc:id:"
            0x1F -> "urn:epc:tag:"
            0x20 -> "urn:epc:pat:"
            0x21 -> "urn:epc:raw:"
            0x22 -> "urn:epc:"
            0x23 -> "urn:nfc:"
            else -> ""
        }
    }
    
    // ========== Writing Tags ==========
    
    fun writeTag(tag: Tag, data: Map<String, String>, url: String? = null): Boolean {
        return try {
            val records = mutableListOf<NdefRecord>()
            
            // Add URI record if URL is provided
            if (url != null) {
                val uriRecord = createUriRecord(url)
                if (uriRecord != null) {
                    records.add(uriRecord)
                }
            }
            
            // Add JSON record
            val jsonRecord = createJsonRecord(data)
            if (jsonRecord != null) {
                records.add(jsonRecord)
            }
            
            if (records.isEmpty()) {
                return false
            }
            
            val ndefMessage = NdefMessage(records.toTypedArray())
            
            // Try to write to tag
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (ndef.isWritable && ndef.maxSize >= ndefMessage.toByteArray().size) {
                    ndef.writeNdefMessage(ndefMessage)
                    ndef.close()
                    return true
                }
                ndef.close()
                return false
            } else {
                // Try to format tag
                val ndefFormatable = NdefFormatable.get(tag)
                if (ndefFormatable != null) {
                    ndefFormatable.connect()
                    ndefFormatable.format(ndefMessage)
                    ndefFormatable.close()
                    return true
                }
                return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun createJsonRecord(data: Map<String, String>): NdefRecord? {
        return try {
            val json = gson.toJson(data)
            val payload = json.toByteArray(Charset.forName("UTF-8"))
            
            NdefRecord(
                NdefRecord.TNF_MIME_MEDIA,
                "application/json".toByteArray(Charset.forName("US-ASCII")),
                ByteArray(0),
                payload
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun createUriRecord(uri: String): NdefRecord? {
        return try {
            // Find matching prefix
            var prefixCode = 0x00
            var uriWithoutPrefix = uri
            
            for (code in 0x01..0x23) {
                val prefix = getUriPrefix(code)
                if (uri.startsWith(prefix)) {
                    prefixCode = code
                    uriWithoutPrefix = uri.substring(prefix.length)
                    break
                }
            }
            
            val uriBytes = uriWithoutPrefix.toByteArray(Charset.forName("UTF-8"))
            val payload = ByteArray(uriBytes.size + 1)
            payload[0] = prefixCode.toByte()
            System.arraycopy(uriBytes, 0, payload, 1, uriBytes.size)
            
            NdefRecord(
                NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_URI,
                ByteArray(0),
                payload
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // ========== Utility ==========
    
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        
        for (byte in bytes) {
            val value = byte.toInt()
            result.append(hexChars[value shr 4 and 0x0F])
            result.append(hexChars[value and 0x0F])
        }
        
        return result.toString()
    }
}

/**
 * NFC tag data model
 */
data class NfcTagData(
    val data: Map<String, String>,
    val url: String?,
    val tagId: String
)
