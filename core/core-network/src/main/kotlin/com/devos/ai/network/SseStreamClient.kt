package com.devos.ai.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Named

/**
 * SSE-based streaming client for the DevOS AI backend chat/stream endpoint.
 *
 * Retrofit does not natively handle SSE (text/event-stream), so we use
 * OkHttp's [EventSources] factory directly and expose the token stream
 * as a cold [Flow].
 *
 * The flow emits raw JSON delta strings from the server.  Callers (the
 * data-ai layer) are responsible for parsing the JSON and mapping to
 * domain [ChatMessage] chunks.
 *
 * Security: no auth token is added here — the DevOS backend is public
 * for now.  VCS tokens are passed per-call by higher-level code.
 */
class SseStreamClient @Inject constructor(
    @Named("devos_http_client") private val okHttpClient: OkHttpClient,
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Opens an SSE stream to `{baseUrl}chat/stream` and emits each data
     * chunk as a [String].
     *
     * The flow closes normally when the server sends `[DONE]` or when the
     * connection is cleanly closed.  The flow closes with an exception on
     * network failure.
     *
     * @param baseUrl   Base URL including trailing slash, e.g.
     *                  `http://10.0.2.2:8000/v1/`
     * @param requestBody  JSON-serialised [ChatRequest] body.
     */
    fun streamChat(baseUrl: String, requestBody: String): Flow<String> = callbackFlow {
        val request = Request.Builder()
            .url("${baseUrl}chat/stream")
            .post(requestBody.toRequestBody(jsonMediaType))
            .header("Accept", "text/event-stream")
            .build()

        val factory = EventSources.createFactory(okHttpClient)
        val eventSource = factory.newEventSource(request, object : EventSourceListener() {

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                when {
                    data == "[DONE]" -> close()
                    data.isNotBlank() -> trySend(data)
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?,
            ) {
                val cause = t ?: Exception(
                    "SSE stream failed: HTTP ${response?.code} ${response?.message}",
                )
                close(cause)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        })

        awaitClose { eventSource.cancel() }
    }
}
