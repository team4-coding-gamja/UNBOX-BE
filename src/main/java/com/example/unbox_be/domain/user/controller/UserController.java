package com.example.unbox_be.domain.user.controller;

import com.example.unbox_be.domain.user.controller.api.UserApi;
import com.example.unbox_be.domain.user.dto.request.UserSignupRequestDto;
import com.example.unbox_be.domain.user.dto.response.UserSignupResponseDto;
import com.example.unbox_be.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

   // @GetMapping("")
    //test-gh
   @GetMapping("/api/user/test")
   public String test() {
       return "<h1>Spring Boot + Postgres + Redis 연결 성공!</h1>" +
               "<p>로컬 개발 환경이 완벽하게 세팅되었습니다.</p>";
   }

   @PostMapping("/api/auth/signup")
   public ResponseEntity<UserSignupResponseDto> register(@Valid @RequestBody UserSignupRequestDto userSignupRequestDto) {
       UserSignupResponseDto responseDto = userService.signup(userSignupRequestDto);
       return ResponseEntity.ok(responseDto);
   }
}
