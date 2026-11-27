package org.isfce.pid.service;

import java.util.List;
import java.util.Optional;

import org.isfce.pid.dao.IUserDao;
import org.isfce.pid.dto.UserDto;
import org.isfce.pid.model.User;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class UserService {

	private final IUserDao userDao;

	public UserService(IUserDao userDao) {
		this.userDao = userDao;
	}

	public List<UserDto> getAllUsersDto() {
		return userDao.getAllUserDto();
	}

	public Optional<User> findUserByUsername(String username) {
		return userDao.findById(username);
	}

	public User saveUser(User user) {
		return userDao.save(user);
	}
}
