package com.inter.efficientia_mobile.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/** Contract from the ultrathink API map: POST /chat. */
public interface CinttiaService {
    @POST("chat")
    Call<ChatEnvelope> chat(@Body ChatPayload payload);

    final class ChatPayload {
        public final String user_id;
        public final String session_id;
        public final String message;

        public ChatPayload(String userId, String sessionId, String message) {
            this.user_id = userId;
            this.session_id = sessionId;
            this.message = message;
        }
    }

    final class ChatEnvelope {
        public ChatResult response;
    }

    final class ChatResult {
        public String resposta;
    }
}
