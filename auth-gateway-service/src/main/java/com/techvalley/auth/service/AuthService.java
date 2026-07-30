package com.techvalley.auth.service;

import com.techvalley.auth.dto.request.LoginRequest;
import com.techvalley.auth.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

}