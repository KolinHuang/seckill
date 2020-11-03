package com.yucaihuang.controller;

import com.yucaihuang.dto.Exposer;
import com.yucaihuang.dto.SeckillExecution;
import com.yucaihuang.dto.SeckillResult;
import com.yucaihuang.enums.SeckillStatEnum;
import com.yucaihuang.exception.RepeatKillException;
import com.yucaihuang.exception.SeckillCloseException;
import com.yucaihuang.pojo.Seckill;
import com.yucaihuang.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /**
     * 展示秒杀列表
     * @param model
     * @return
     */
    @GetMapping("/list")
    public String list(Model model){
        List<Seckill> seckillList = seckillService.getSeckillList();
        model.addAttribute("seckillList",seckillList);
        return "list";
    }

    /**
     * 秒杀商品详情页
     * @param seckillId
     * @param model
     * @return
     */
    @GetMapping("/{seckillId}/detail")
    public String detail(@PathVariable("seckillId") Long seckillId, Model model){
        if(seckillId == null){
            return "redirect:/seckill/list";
        }

        Seckill seckill = seckillService.getSeckillById(seckillId);
        if(seckill == null){
            return "forward:/seckill/list";
        }
        model.addAttribute("seckill",seckill);

        return "detail";
    }

    /**
     * 返回一个JSON数据，数据中封装了我们商品的秒杀地址
     * @param seckillId
     * @return
     */
    @GetMapping(value = "/{seckillId}/exposer", produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<Exposer> exposer(@PathVariable("seckillId") Long seckillId){
        SeckillResult<Exposer> result;
        try {
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
            //成功取到了暴露的地址
            result = new SeckillResult<Exposer>(true, exposer);
        } catch (Exception e) {
            e.printStackTrace();
            //取地址失败了，封装异常信息
            result = new SeckillResult<Exposer>(false,e.getMessage());
        }
        return result;
    }

    /**
     * 用于封装用户是否秒杀成功的信息
     * @param secKillId
     * @param md5
     * @return
     */
    @PostMapping(value = "/{seckillId}/{md5}/execution",
    produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId") Long secKillId,
                                                   @PathVariable("md5") String md5,
                                                   @CookieValue(value = "userPhone", required = false) Long userPhone){

        if(userPhone == null){
            return new SeckillResult<SeckillExecution>(false,"未注册");
        }
        SeckillResult<SeckillExecution> result;

        try {
            SeckillExecution execution = seckillService.executeSeckill(secKillId, userPhone, md5);
            return new SeckillResult<SeckillExecution>(true,execution);
        }catch (RepeatKillException e1){
            SeckillExecution execution = new SeckillExecution(secKillId, SeckillStatEnum.REPEAT_KILL);
            return new SeckillResult<SeckillExecution>(true,execution);
        }catch (SeckillCloseException e2){
            SeckillExecution execution = new SeckillExecution(secKillId, SeckillStatEnum.END);
            return new SeckillResult<SeckillExecution>(true,execution);
        }catch (Exception e){
            SeckillExecution execution = new SeckillExecution(secKillId, SeckillStatEnum.INNER_ERROR);
            return new SeckillResult<SeckillExecution>(true,execution);
        }
    }

    /**
     * 返回系统当前时间
     * @return
     */
    @GetMapping("/time/now")
    @ResponseBody
    public SeckillResult<Long> time(){
        Date date = new Date();
        return new SeckillResult<Long>(true, date.getTime());
    }

}
