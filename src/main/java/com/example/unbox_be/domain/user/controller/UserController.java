package com.example.unbox_be.domain.user.controller;

import com.example.unbox_be.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

   // @GetMapping("")
    //test-gh
   @GetMapping("/api/user/test")
   public String test() {
       return "<h1>Spring Boot + Postgres + Redis 연결 성공!</h1>" +
               "<p>로컬 개발 환경이 완벽하게 세팅되었습니다.</p>";
   }
   // @PostMapping("")

}
