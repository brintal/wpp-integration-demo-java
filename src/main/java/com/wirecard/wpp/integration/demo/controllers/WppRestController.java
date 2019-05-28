package com.wirecard.wpp.integration.demo.controllers;


import com.wirecard.wpp.integration.demo.ConfigProperties;
import com.wirecard.wpp.integration.demo.LastTransactionStore;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;


@RestController
public class WppRestController {

    private static final String REGISTER = "/api/register";
    private static final String RECUR_PAYMENT = "/api/recurPayment";
    private static final String SEAMLESS = "seamless";
    private static final String EMBEDDED = "embedded";
    private static final String RECUR = "recur";
    private static final String EPS = "eps";

    @Value("${wpp.gui.flow.base.endpoint}")
    private String wppGuiFlowBaseEndpoint;
    @Value("${wpp.rest.api.base.endpoint}")
    private String wppRestApiBaseEndpoint;

    @Value("${frame.ancestor}")
    private String frameAncestor;

    @Autowired
    private ConfigProperties.WirecardConfig ccConfig;
    @Autowired
    private ConfigProperties.WirecardConfig epsConfig;
    @Autowired
    private LastTransactionStore lastTransactionStore;


    public WppRestController() {
    }

    private JSONObject getPaymentModel(String mode, String paymentMethod, ConfigProperties.WirecardConfig config, String requestTemplate) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            String payment = IOUtils.toString(classLoader.getResourceAsStream("payment" + File.separator + requestTemplate));

            JSONObject json = new JSONObject(payment);

            json.getJSONObject("payment").put("request-id", UUID.randomUUID());
            json.getJSONObject("payment").getJSONObject("merchant-account-id").put("value", config.getMaid());
            if (mode != null && !mode.contains(RECUR)) {
                json.getJSONObject("payment")
                        .getJSONObject("payment-methods")
                        .getJSONArray("payment-method").put(new JSONObject("{name:" + paymentMethod + "}"));
            }

            if (mode != null && mode.contains(RECUR)) {
                json.getJSONObject("payment").put("parent-transaction-id", lastTransactionStore.getLastTransactionId());
            }

            if (mode != null && mode.contains(SEAMLESS)) {
                json.getJSONObject("options").put("mode", SEAMLESS);
            }
            // frame-ancestor must be set for SEAMLESS mode and EMBEDDED integration
            if (null != mode && (mode.contains(SEAMLESS) || mode.contains(EMBEDDED))) {
                json.getJSONObject("options").put("frame-ancestor", frameAncestor);
            }
            return json;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = REGISTER, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String registerPayment(HttpServletRequest request, HttpServletResponse response) {
        return processRequest((request1, response1) -> {
            String jsonRaw = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            JSONObject req = new JSONObject(jsonRaw);
            String mode = req.get("mode").toString();
            String requestTemplate = mode.contains(EPS) ? "paymenteps.json" : "payment.json";
            String paymentMethod = req.get("payment-method").toString();
            ConfigProperties.WirecardConfig config = mode.contains(EPS) ? epsConfig : ccConfig;
            JSONObject json = getPaymentModel(mode, paymentMethod, config, requestTemplate);
            return sendRequest(response, json, config, wppGuiFlowBaseEndpoint + "/register");
        }, request, response);

    }

    @RequestMapping(value = RECUR_PAYMENT, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String recurPayment(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return processRequest((request1, response1) -> {
            String jsonRaw = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            JSONObject req = new JSONObject(jsonRaw);
            String mode = req.get("mode").toString();
            String paymentMethod = req.get("payment-method").toString();
            JSONObject json = getPaymentModel(mode, paymentMethod, ccConfig, "paymentrecur.json");
            return sendRequest(response, json, ccConfig, wppRestApiBaseEndpoint);
        }, request, response);
    }

    private String processRequest(RequestProcessor requestProcessor, HttpServletRequest request, HttpServletResponse response) {
        try {
            return requestProcessor.process(request, response);
        } catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException he = (HttpClientErrorException) e;
                response.setStatus(he.getStatusCode().value());
                return he.getResponseBodyAsString();
            }
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return null;
    }

    private String sendRequest(HttpServletResponse response, JSONObject json, ConfigProperties.WirecardConfig config, String endpoint) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((config.getUsername() + ":" + config.getPassword()).getBytes()));
        HttpEntity<String> request2WPP = new HttpEntity<>(json.toString(), headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(endpoint, request2WPP, String.class);
        response.setStatus(responseEntity.getStatusCodeValue());
        return responseEntity.getBody();
    }

    @FunctionalInterface
    private interface RequestProcessor {
        String process(HttpServletRequest request, HttpServletResponse response) throws Exception;
    }
}