package com.nnngu.service;

import com.nnngu.dao.SeckillMapper;
import com.nnngu.dao.SuccessKilledMapper;
import com.nnngu.dao.cache.RedisDao;
import com.nnngu.dto.Exposer;
import com.nnngu.dto.SeckillExecution;
import com.nnngu.entity.Seckill;
import com.nnngu.entity.SuccessKilled;
import com.nnngu.enums.SeckillStatEnum;
import com.nnngu.exception.RepeatKillException;
import com.nnngu.exception.SeckillCloseException;
import com.nnngu.exception.SeckillException;
import com.nnngu.service.interfaces.SeckillService;
import com.nnngu.service.redis.RedisService;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nnngu.enums.SeckillStatEnum.SUCCESS;

/**
 * Created by nnngu
 */
@Service
public class SeckillServiceImpl implements SeckillService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    /* 加入一个盐值,用于混淆*/
    private final String salt = "thisIsASaltValue";

    @Autowired
    private SeckillMapper seckillMapper;
    @Autowired
    private SuccessKilledMapper successKilledMapper;
    @Autowired
    private RedisService redisServiceImpl;



    /**
     * 查询全部的秒杀记录.
     *
     * @return 数据库中所有的秒杀记录
     */
    @Override
    public List<Seckill> getSeckillList() {
        return seckillMapper.queryAll(0, 4);
    }

    /**
     * 查询单个秒杀记录
     *
     * @param seckillId 秒杀记录的ID
     * @return 根据ID查询出来的记录信息
     */
    @Override
    public Seckill getById(long seckillId) {
        return seckillMapper.queryById(seckillId);
    }

    /**
     * 在秒杀开启时输出秒杀接口的地址，否则输出系统时间跟秒杀地址
     *
     * @param seckillId 秒杀商品Id
     * @return 根据对应的状态返回对应的状态实体
     */
    @Override
    public Exposer exportSeckillUrl(long seckillId) {
        // 根据秒杀的ID去查询是否存在这个商品
       /* Seckill seckill = seckillMapper.queryById(seckillId);
        if (seckill == null) {
            logger.warn("查询不到这个秒杀产品的记录");
            return new Exposer(false, seckillId);
        }*/
        Seckill seckill = redisServiceImpl.get(seckillId);
        System.out.println("----ID：---"+seckill+"--------");
        if (seckill == null) {
            // 访问数据库读取数据
            seckill = seckillMapper.queryById(seckillId);
            if (seckill == null) {
                return new Exposer(false, seckillId);
            } else {
                System.out.println("写入redis");
                // 放入redis
                redisServiceImpl.put(seckillId,seckill,300);
            }
        }

        // 判断是否还没到秒杀时间或者是过了秒杀时间
        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        Date nowTime = new Date();
        // 开始时间大于现在的时候说明没有开始秒杀活动；秒杀活动结束时间小于现在的时间说明秒杀已经结束了
        if (nowTime.getTime() > startTime.getTime() && nowTime.getTime() < endTime.getTime()) {
            //秒杀开启,返回秒杀商品的id,用给接口加密的md5
            String md5 = getMd5(seckillId);
            return new Exposer(true, md5, seckillId);
        }
        return new Exposer(false, seckillId, nowTime.getTime(), startTime.getTime(), endTime.getTime());

    }

    /**
     * MD5 + 盐值 加密
     * @param seckillId 秒杀商品的id
     * @return
     */
    private String getMd5(long seckillId) {
        String base = seckillId + "/" + salt;
        return DigestUtils.md5DigestAsHex(base.getBytes());
    }

    /**
     * 执行秒杀操作，失败我们就抛出异常
     *
     * @param seckillId 秒杀的商品ID
     * @param userPhone 手机号码
     * @param md5       md5加密值
     * @return 根据不同的结果返回不同的实体信息
     */
    @Transactional
    @Override
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException {

//        if (md5 == null || !md5.equals(getMd5(seckillId))) {
//            logger.error("秒杀数据被篡改");
//            throw new SeckillException("seckill data rewrite");
//        }
//        // 执行秒杀业务逻辑
//        Date nowTime = new Date();
//
//        try {
//            //执行减库存操作
//            int reduceNumber = seckillMapper.reduceNumber(seckillId, nowTime);
//            if (reduceNumber <= 0) {
//                logger.warn("没有更新数据库记录,说明秒杀结束");
//                throw new SeckillCloseException("seckill is closed");
//            } else {
//                // 这里至少减少的数量不为0了,秒杀成功了就增加一个秒杀成功详细
//                int insertCount = successKilledMapper.insertSuccessKilled(seckillId, userPhone);
//                // 查看是否被重复插入,即用户是否重复秒杀
//                if (insertCount <= 0) {
//                    throw new RepeatKillException("seckill repeated");
//                } else {
//                    // 秒杀成功了,返回那条插入成功秒杀的信息
//                    SuccessKilled successKilled = successKilledMapper.queryByIdWithSeckill(seckillId, userPhone);
//                    return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS, successKilled);
//                }
//            }
//        } catch (SeckillCloseException | RepeatKillException e1) {
//            throw e1;
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//            // 把编译期异常转换为运行时异常
//            throw new SeckillException("seckill inner error : " + e.getMessage());
//        }

        if (md5 == null || !md5.equals(getMd5(seckillId))) {
            logger.error("秒杀数据被篡改");
            throw new SeckillException("seckill data rewrite");
        }
        // 执行秒杀业务逻辑
        Date nowTime = new Date();

        try {
            // 记录购买行为
            int insertCount = successKilledMapper.insertSuccessKilled(seckillId, userPhone);
            if (insertCount <= 0) {
                // 重复秒杀
                throw new RepeatKillException("seckill repeated");
            } else {
                // 减库存 ,热点商品的竞争
                int reduceNumber = seckillMapper.reduceNumber(seckillId, nowTime);
                if (reduceNumber <= 0) {
                    logger.warn("没有更新数据库记录,说··明秒杀结束");
                    throw new SeckillCloseException("seckill is closed");
                } else {
                    // 秒杀成功了,返回那条插入成功秒杀的信息  进行commit
                    SuccessKilled successKilled = successKilledMapper.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS, successKilled);
                }
            }
        } catch (SeckillCloseException | RepeatKillException e1) {
            throw e1;
        }

    }

    @Override
    public SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5) {
        if (md5 == null || !md5.equals(getMd5(seckillId))) {
            return new SeckillExecution(seckillId, SeckillStatEnum.DATE_REWRITE);
        }
        Date killTime = new Date();
        Map<String, Object> map = new HashMap<>();
        map.put("seckillId", seckillId);
        map.put("phone", userPhone);
        map.put("killTime", killTime);
        map.put("result", null);
        // 执行储存过程，result被复制
        try {
            seckillMapper.killByProcedure(map);
            // 获取result
            int result = MapUtils.getInteger(map, "result", -2);
            if (result == 1) {
                SuccessKilled successKilled = successKilledMapper.queryByIdWithSeckill(seckillId, userPhone);
                return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS, successKilled);
            } else {
                return new SeckillExecution(seckillId, SeckillStatEnum.stateOf(result));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new SeckillExecution(seckillId, SeckillStatEnum.INNER_ERROR);
        }
    }

}
