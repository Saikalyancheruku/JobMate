package com.saikalyan.jobmate.data.remote

import com.saikalyan.jobmate.data.secure.SmtpCredential
import com.saikalyan.jobmate.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties
import javax.inject.Inject
import javax.mail.AuthenticationFailedException
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.activation.DataHandler
import javax.activation.FileDataSource

sealed interface SmtpResult {
    data object Connected : SmtpResult
    data object Sent : SmtpResult
    data class Failed(val reason: SmtpFailure) : SmtpResult
    data class Unknown(val reason: SmtpFailure) : SmtpResult
}

enum class SmtpFailure { AUTH_REJECTED, INVALID_RECIPIENT, NETWORK, TLS, ATTACHMENT, UNKNOWN }

/** Gmail SMTP only. Each send accepts exactly one recipient and never retries. */
class GmailSmtpClient @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun testConnection(credential: SmtpCredential): SmtpResult = withContext(ioDispatcher) {
        var transport: Transport? = null
        try {
            val session = Session.getInstance(properties())
            transport = session.getTransport("smtp")
            transport.connect(HOST, PORT, credential.email, credential.appPassword)
            SmtpResult.Connected
        } catch (failure: Exception) {
            SmtpResult.Failed(classify(failure))
        } finally {
            runCatching { transport?.close() }
        }
    }

    suspend fun sendOne(
        credential: SmtpCredential,
        recipient: String,
        subject: String,
        body: String,
        attachment: File? = null,
        attachmentName: String? = null,
    ): SmtpResult = withContext(ioDispatcher) {
        val address = try { InternetAddress(recipient, true).apply { validate() } }
        catch (_: AddressException) { return@withContext SmtpResult.Failed(SmtpFailure.INVALID_RECIPIENT) }
        if (attachment != null && (!attachment.isFile || !attachment.canRead())) {
            return@withContext SmtpResult.Failed(SmtpFailure.ATTACHMENT)
        }
        var transport: Transport? = null
        var submitting = false
        try {
            val session = Session.getInstance(properties())
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(credential.email, true))
                setRecipient(Message.RecipientType.TO, address)
                setSubject(subject, "UTF-8")
                if (attachment == null) {
                    setText(body, "UTF-8")
                } else {
                    val multipart = MimeMultipart()
                    multipart.addBodyPart(MimeBodyPart().apply { setText(body, "UTF-8") })
                    multipart.addBodyPart(MimeBodyPart().apply {
                        dataHandler = DataHandler(FileDataSource(attachment))
                        fileName = attachmentName?.takeIf(String::isNotBlank) ?: "resume.pdf"
                    })
                    setContent(multipart)
                }
                saveChanges()
            }
            transport = session.getTransport("smtp")
            transport.connect(HOST, PORT, credential.email, credential.appPassword)
            submitting = true
            transport.sendMessage(message, arrayOf(address))
            SmtpResult.Sent
        } catch (failure: Exception) {
            val reason = classify(failure)
            if (submitting && reason !in setOf(SmtpFailure.AUTH_REJECTED, SmtpFailure.INVALID_RECIPIENT, SmtpFailure.ATTACHMENT)) {
                SmtpResult.Unknown(reason)
            } else SmtpResult.Failed(reason)
        } finally {
            runCatching { transport?.close() }
        }
    }

    private fun properties() = Properties().apply {
        setProperty("mail.smtp.host", HOST)
        setProperty("mail.smtp.port", PORT.toString())
        setProperty("mail.smtp.auth", "true")
        setProperty("mail.smtp.starttls.enable", "true")
        setProperty("mail.smtp.starttls.required", "true")
        setProperty("mail.smtp.ssl.checkserveridentity", "true")
        setProperty("mail.smtp.connectiontimeout", TIMEOUT_MS.toString())
        setProperty("mail.smtp.timeout", TIMEOUT_MS.toString())
        setProperty("mail.smtp.writetimeout", TIMEOUT_MS.toString())
        setProperty("mail.debug", "false")
    }

    private fun classify(failure: Throwable): SmtpFailure = when {
        failure is AuthenticationFailedException -> SmtpFailure.AUTH_REJECTED
        generateSequence(failure) { it.cause }.any { it is AddressException } -> SmtpFailure.INVALID_RECIPIENT
        generateSequence(failure) { it.cause }.any { it.message?.contains("TLS", ignoreCase = true) == true || it.message?.contains("STARTTLS", ignoreCase = true) == true } -> SmtpFailure.TLS
        failure is MessagingException || failure is java.io.IOException -> SmtpFailure.NETWORK
        else -> SmtpFailure.UNKNOWN
    }

    private companion object {
        const val HOST = "smtp.gmail.com"
        const val PORT = 587
        const val TIMEOUT_MS = 15_000
    }
}
