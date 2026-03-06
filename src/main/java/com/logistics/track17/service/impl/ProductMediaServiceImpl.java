package com.logistics.track17.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.logistics.track17.entity.ProductMedia;
import com.logistics.track17.mapper.ProductMediaMapper;
import com.logistics.track17.service.ProductMediaService;
import org.springframework.stereotype.Service;

@Service
public class ProductMediaServiceImpl extends ServiceImpl<ProductMediaMapper, ProductMedia>
        implements ProductMediaService {
}
