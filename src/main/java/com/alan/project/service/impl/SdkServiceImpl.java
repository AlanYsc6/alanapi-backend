package com.alan.project.service.impl;

import com.alan.project.mapper.SdkMapper;
import com.alan.project.model.entity.Sdk;
import com.alan.project.service.SdkService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * SDK 下载服务实现
 *
 * @author alan
 */
@Service
public class SdkServiceImpl extends ServiceImpl<SdkMapper, Sdk> implements SdkService {
}
