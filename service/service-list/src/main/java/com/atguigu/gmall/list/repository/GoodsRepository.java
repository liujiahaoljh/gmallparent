package com.atguigu.gmall.list.repository;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author jiahao
 * @create 2020-03-23 22:27
 */
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}
