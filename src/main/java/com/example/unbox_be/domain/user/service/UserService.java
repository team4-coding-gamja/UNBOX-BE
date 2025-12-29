package com.example.unbox_be.domain.user.service;

import com.example.unbox_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // save

    // read

    // update - 업데이트는 transactional 명시 안해도됨

    // delete

}
