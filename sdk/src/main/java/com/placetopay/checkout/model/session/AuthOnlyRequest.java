package com.placetopay.checkout.model.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.placetopay.checkout.auth.Auth;

/**
 * Request body used by query and cancel — carries only the auth block. Not part of the public API;
 * the SDK constructs it internally.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthOnlyRequest(Auth auth) {}
