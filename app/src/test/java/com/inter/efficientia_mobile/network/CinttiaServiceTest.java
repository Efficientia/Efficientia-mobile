package com.inter.efficientia_mobile.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import retrofit2.http.POST;

public class CinttiaServiceTest {
    @Test public void chatPayloadMatchesApiMap() throws Exception {
        String json = new Gson().toJson(new CinttiaService.ChatPayload("12", "session", "Olá"));
        assertTrue(json.contains("\"user_id\":\"12\""));
        assertTrue(json.contains("\"session_id\":\"session\""));
        assertTrue(json.contains("\"message\":\"Olá\""));
        assertEquals("chat", CinttiaService.class.getMethod("chat", CinttiaService.ChatPayload.class)
                .getAnnotation(POST.class).value());
    }

    @Test public void chatResponseMatchesApiMap() {
        CinttiaService.ChatEnvelope envelope = new Gson().fromJson(
                "{\"response\":{\"resposta\":\"Tudo certo\",\"agentes_chamados\":[\"faq\"]}}",
                CinttiaService.ChatEnvelope.class);
        assertEquals("Tudo certo", envelope.response.resposta);
    }
}
