package com.example.bankcards.util;

import com.example.bankcards.dto.UserAuthDTO;
import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.User;
import org.mapstruct.*;

@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class UserMapper {

    @Mapping(target = "encryptedPassword", source = "password")
    public abstract User map(UserAuthDTO dto);

    public abstract UserDTO map(User model);
}
