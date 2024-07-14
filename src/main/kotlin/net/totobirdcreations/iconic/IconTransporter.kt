package net.totobirdcreations.iconic

import net.minecraft.util.JsonHelper
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLConnection


const val MEGABYTE : Int = 1000000


object IconTransporter {

    private const val CHARSET : String = "UTF-8";
    private const val CRLF    : String = "\r\n";

    const val MAX_BYTES : Int = MEGABYTE / 2; // 0.5 MiB
    const val MAX_SIZE  : Int = 64;

    private val DOWNLOAD_URL : Regex = Regex("^https:\\/\\/tmpfiles.org/([0-9]+)/.*$");


    @OptIn(ExperimentalStdlibApi::class)
    fun uploadIcon(filename : String, file : ByteArray) : Result<String> {
        try {
            if (file.size > MAX_BYTES) {
                return Result.failure(Exception("File too big."));
            }

            val boundary = System.currentTimeMillis().toHexString();

            // Set up the connection.
            val url = URI("https://tmpfiles.org/api/v1/upload").toURL();
            val conn = url.openConnection() as HttpURLConnection;
            conn.useCaches = false;
            conn.doOutput = true;
            conn.doInput = true;
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=${boundary}");
            conn.setRequestProperty("User-Agent", "IconicMC");

            // Add the file.
            val outStream = conn.outputStream;
            val writer = PrintWriter(OutputStreamWriter(outStream, CHARSET));
            writer.append("--${boundary}"                                                        ).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"icon.dat\"" ).append(CRLF);
            writer.append("Content-Type: ${URLConnection.guessContentTypeFromName(filename)}"    ).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary"                                    ).append(CRLF);
            writer.append(CRLF).flush();
            outStream.write(file);
            outStream.flush();
            writer.append(CRLF).flush();

            // Complete the request.
            writer.append(CRLF).flush();
            writer.append("--${boundary}").append(CRLF);
            writer.close();

            // Get response.
            val success = conn.responseCode == HttpURLConnection.HTTP_OK;
            val inStream = if (success) { conn.inputStream } else { conn.errorStream };
            val response = JsonHelper.deserialize(inStream.readAllBytes().decodeToString(), true);

            // Close the connection.
            inStream.close();
            conn.disconnect();

            // Return response.
            if (! success) { return Result.failure(Exception(response.toString())); }
            val iconUrl = response.getAsJsonObject("data").get("url").asString;
            return Result.success(
                (DOWNLOAD_URL.find(iconUrl)
                    ?: return Result.failure(Throwable("Returned download URI is invalid."))
                ).groups[1]!!.value
            );
        } catch (e : Exception) {
            return Result.failure(e);
        }

    }


    fun downloadIcon(transportId : String) : Result<ByteArray> {
        try {
            // Set up the connection.
            val url = URI("https://tmpfiles.org/dl/${transportId}/icon.dat").toURL();
            val conn = url.openConnection() as HttpURLConnection;
            conn.useCaches = false;
            conn.doInput = true;

            // Get response.
            val success = conn.responseCode == HttpURLConnection.HTTP_OK;
            val inStream = if (success) { conn.inputStream } else { conn.errorStream };
            val data = inStream.readAllBytes();
            if (! success) { return Result.failure(Exception("Failed to download file: `${data.decodeToString()}`")); };

            // Close the connection.
            inStream.close();
            conn.disconnect();

            // Return response.
            if (data.size > MAX_BYTES) { return Result.failure(Exception("File too big.")); }
            return Result.success(data);
        } catch (e : Exception) {
            return Result.failure(e);
        }
    }


    fun getFileNameParts(filename : String, sep : String = ".") : Pair<String, String> {
        val parts = filename.split(sep).toMutableList();
        val ext   = parts.removeLast();
        return Pair(parts.joinToString(sep), ext);
    }


}