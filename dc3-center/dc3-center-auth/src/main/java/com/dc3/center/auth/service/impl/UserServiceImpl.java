/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.auth.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.auth.mapper.UserMapper;
import com.dc3.center.auth.service.UserService;
import com.dc3.common.annotation.Logs;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.CacheConstant;
import com.dc3.common.constant.CommonConstant;
import com.dc3.common.dto.UserDto;
import com.dc3.common.exception.*;
import com.dc3.common.model.User;
import com.dc3.common.utils.Dc3Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * 用户服务接口实现类
 *
 * @author pnoker
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = CacheConstant.Entity.USER + CacheConstant.Suffix.ID, key = "#user.id", condition = "#result!=null"),
                    @CachePut(value = CacheConstant.Entity.USER + CacheConstant.Suffix.NAME, key = "#user.name", condition = "#result!=null"),
                    @CachePut(value = CacheConstant.Entity.USER + CacheConstant.Suffix.PHONE, key = "#user.phone", condition = "#result!=null&&#user.phone!=null"),
                    @CachePut(value = CacheConstant.Entity.USER + CacheConstant.Suffix.EMAIL, key = "#user.email", condition = "#result!=null&&#user.email!=null")
            },
            evict = {
                    @CacheEvict(value = CacheConstant.Entity.USER + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.USER + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    @Logs("Add user")
    @Transactional
    // 2022-03-13 检查：不通过，会返回密码数据
    public User add(User user) {
        // 判断用户是否存在
        User selectByName = selectByName(user.getName(), false);
        if (ObjectUtil.isNotNull(selectByName)) {
            throw new DuplicateException("The user already exists with username: {}", user.getName());
        }

        // 判断 phone 是否存在，如果有 phone 不为空，检查该 phone 是否被占用
        if (StrUtil.isNotEmpty(user.getPhone())) {
            User selectByPhone = selectByPhone(user.getPhone(), false);
            if (ObjectUtil.isNotNull(selectByPhone)) {
                throw new DuplicateException("The user already exists with phone: {}", user.getPhone());
            }
        }

        // 判断 email 是否存在，如果有 email 不为空，检查该 email 是否被占用
        if (StrUtil.isNotEmpty(user.getEmail())) {
            User selectByEmail = selectByEmail(user.getEmail(), false);
            if (ObjectUtil.isNotNull(selectByEmail)) {
                throw new DuplicateException("The user already exists with email: {}", user.getEmail());
            }
        }

        // 插入 user 数据，并返回插入后的 user
        if (userMapper.insert(user.setPassword(Dc3Util.md5(user.getPassword()))) > 0) {
            return userMapper.selectById(user.getId());
        }

        throw new AddException("The user add failed: {}", user.toString());
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = CacheConstant.Entity.USER + CacheConstant.Suffix.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.USER + CacheConstant.Suffix.NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.USER + CacheConstant.Suffix.PHONE, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.USER + CacheConstant.Suffix.EMAIL, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.USER + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.USER + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result==true")
            }
    )
    @Transactional
    public boolean delete(Long id) {
        User user = selectById(id);
        if (null == user) {
            throw new NotFoundException("The user does not exist");
        }
        return userMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = CacheConstant.Entity.USER + CacheConstant.Suffix.ID, key = "#user.id", condition = "#result!=null"),
                    @CachePut(value = CacheConstant.Entity.USER + CacheConstant.Suffix.NAME, key = "#user.name", condition = "#result!=null"),
                    @CachePut(value = CacheConstant.Entity.USER + CacheConstant.Suffix.PHONE, key = "#user.phone", condition = "#result!=null&&#user.phone!=null"),
                    @CachePut(value = CacheConstant.Entity.USER + CacheConstant.Suffix.EMAIL, key = "#user.email", condition = "#result!=null&&#user.email!=null")
            },
            evict = {
                    @CacheEvict(value = CacheConstant.Entity.USER + CacheConstant.Suffix.PHONE, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.USER + CacheConstant.Suffix.EMAIL, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.USER + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.USER + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    @Transactional
    public User update(User user) {
        User byId = selectById(user.getId());
        // 判断 phone 是否修改
        if (StrUtil.isNotBlank(user.getPhone())) {
            if (null == byId.getPhone() || !byId.getPhone().equals(user.getPhone())) {
                if (null != selectByPhone(user.getPhone(), false)) {
                    throw new DuplicateException("The user already exists with phone {}", user.getPhone());
                }
            }
        } else {
            user.setPhone(null);
        }

        // 判断 email 是否修改
        if (StrUtil.isNotBlank(user.getEmail())) {
            if (null == byId.getEmail() || !byId.getEmail().equals(user.getEmail())) {
                if (null != selectByEmail(user.getEmail(), false)) {
                    throw new DuplicateException("The user already exists with email {}", user.getEmail());
                }
            }
        } else {
            user.setEmail(null);
        }

        user.setName(null).setUpdateTime(null);
        if (userMapper.updateById(user) > 0) {
            User select = userMapper.selectById(user.getId());
            user.setName(select.getName());
            return select;
        }
        throw new ServiceException("The user update failed");
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.USER + CacheConstant.Suffix.ID, key = "#id", unless = "#result==null")
    public User selectById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.USER + CacheConstant.Suffix.NAME, key = "#name", unless = "#result==null")
    // 2022-03-13 检查：通过
    public User selectByName(String name, boolean isEx) {
        if (StrUtil.isEmpty(name)) {
            if (isEx) {
                throw new EmptyException("The name is empty");
            }
            return null;
        }

        return selectByKey(User::getName, name, isEx);
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.USER + CacheConstant.Suffix.PHONE, key = "#phone", unless = "#result==null")
    // 2022-03-13 检查：通过
    public User selectByPhone(String phone, boolean isEx) {
        if (StrUtil.isEmpty(phone)) {
            if (isEx) {
                throw new EmptyException("The phone is empty");
            }
            return null;
        }

        return selectByKey(User::getPhone, phone, isEx);
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.USER + CacheConstant.Suffix.EMAIL, key = "#email", unless = "#result==null")
    // 2022-03-13 检查：通过
    public User selectByEmail(String email, boolean isEx) {
        if (StrUtil.isEmpty(email)) {
            if (isEx) {
                throw new EmptyException("The phone is empty");
            }
            return null;
        }

        return selectByKey(User::getEmail, email, isEx);
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.USER + CacheConstant.Suffix.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<User> list(UserDto userDto) {
        if (!Optional.ofNullable(userDto.getPage()).isPresent()) {
            userDto.setPage(new Pages());
        }
        return userMapper.selectPage(userDto.getPage().convert(), fuzzyQuery(userDto));
    }

    @Override
    public boolean checkUserValid(String name) {
        User user = selectByName(name, false);
        if (null != user) {
            return user.getEnable();
        }

        user = selectByPhone(name, false);
        if (null != user) {
            return user.getEnable();
        }

        user = selectByEmail(name, false);
        if (null != user) {
            return user.getEnable();
        }

        return false;
    }

    @Override
    public boolean restPassword(Long id) {
        User user = selectById(id);
        if (null != user) {
            user.setPassword(Dc3Util.md5(CommonConstant.Algorithm.DEFAULT_PASSWORD));
            return null != update(user);
        }
        return false;
    }

    @Override
    public LambdaQueryWrapper<User> fuzzyQuery(UserDto userDto) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>query().lambda();
        if (null != userDto) {
            if (StrUtil.isNotBlank(userDto.getName())) {
                queryWrapper.like(User::getName, userDto.getName());
            }
        }
        return queryWrapper;
    }

    private User selectByKey(SFunction<User, ?> key, String value, boolean isEx) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>query().lambda();
        queryWrapper.eq(key, value);
        User user = userMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(user)) {
            if (isEx) {
                throw new NotFoundException("The user does not exist");
            }
            return null;
        }
        return user;
    }

}
