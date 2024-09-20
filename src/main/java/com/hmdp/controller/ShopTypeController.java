package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IShopTypeService;
import com.hmdp.service.impl.ShopTypeServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;
    @Autowired
    private ShopTypeServiceImpl shopTypeServiceImpl;

    @GetMapping("list")
    public Result queryTypeList() {
        return shopTypeServiceImpl.queryTypeList();
    }
}
