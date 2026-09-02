package com.alan.project.service.impl;

import com.alan.project.mapper.DocMapper;
import com.alan.project.model.entity.Doc;
import com.alan.project.service.DocService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 首页文档服务实现
 *
 * @author alan
 */
@Service
public class DocServiceImpl extends ServiceImpl<DocMapper, Doc> implements DocService {
}
