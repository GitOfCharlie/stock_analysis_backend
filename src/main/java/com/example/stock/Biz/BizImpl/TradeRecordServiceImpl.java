package com.example.stock.Biz.BizImpl;

import com.example.stock.Biz.TradeRecordService;
import com.example.stock.Biz.TradeRecordServiceForBl;
import com.example.stock.DO.KLineRequest;
import com.example.stock.DO.RatioVO;
import com.example.stock.DO.TradeRecord;
import com.example.stock.Form.KLine;
import com.example.stock.Form.KLineRequestForm;
import com.example.stock.Mapper.TradeRecordMapper;
import com.example.stock.Utils.DateUtil;
import com.example.stock.Utils.UtilDO.DateTimeRange;
import com.example.stock.VO.ResponseVO;
import com.example.stock.VO.TradeRecordVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @ Description
 * @ Author CaoYang
 * @ Date 2020/12/13
 */
@Service
public class TradeRecordServiceImpl implements TradeRecordService, TradeRecordServiceForBl {

    private static final String NO_SUCH_STOCK_CODE_ERR = "股票代码不存在";
    private static final String NO_SUCH_SCHEMA_ERR = "数据表不存在";

    @Autowired
    private TradeRecordMapper tradeRecordMapper;

    @Override
    public ResponseVO getKLineData(KLineRequestForm kLineRequestForm){
        dateTimeToWorkday(kLineRequestForm);

        List<TradeRecord> tradeRecordList = kLineDispatcher(kLineRequestForm);
        if(tradeRecordList == null){
            return ResponseVO.buildFailure("未知K线类型");
        }

        List<TradeRecordVO> tradeRecordVOList = new ArrayList<>();
        for(TradeRecord tradeRecord: tradeRecordList){
            TradeRecordVO tradeRecordVO = new TradeRecordVO();
            BeanUtils.copyProperties(tradeRecord, tradeRecordVO);
            tradeRecordVOList.add(tradeRecordVO);
        }
        return ResponseVO.buildSuccess(tradeRecordVOList);
    }

    private void dateTimeToWorkday(KLineRequestForm form){
        //日k不处理
        if(form.getKLine() == KLine.K_1D){
            return;
        }
        DateTimeRange dateTimeRange =
                DateUtil.parseDateToNearestWorkday(form.getFromDate(), form.getToDate());
        if(dateTimeRange != null) {
            form.setFromDate(dateTimeRange.getFromDateTime());
            form.setToDate(dateTimeRange.getToDateTime());
        }
    }

    private List<TradeRecord> kLineDispatcher(KLineRequestForm kLineRequestForm){
        KLineRequest kLineRequest = new KLineRequest(kLineRequestForm);
        List<TradeRecord> tradeRecordList;

        KLine kLineType = kLineRequestForm.getKLine();
        switch (kLineType){
            case K_5MIN:
                tradeRecordList = tradeRecordMapper.getTradeRecord_5min(kLineRequest);
                break;
            case K_15MIN:
                tradeRecordList = tradeRecordMapper.getTradeRecord_15min(kLineRequest);
                break;
            case K_30MIN:
                tradeRecordList = tradeRecordMapper.getTradeRecord_30min(kLineRequest);
                break;
            case K_60MIN:
                tradeRecordList = tradeRecordMapper.getTradeRecord_60min(kLineRequest);
                break;
            case K_1D:
                tradeRecordList = tradeRecordMapper.getTradeRecord_1d(kLineRequest);
                break;
            default:
                return null;
        }
        return tradeRecordList;
    }

    @Override
    public List<TradeRecord> getKLineDataForContrast(KLineRequestForm kLineRequestForm){
        return kLineDispatcher(kLineRequestForm);
    }

    @Override
    public ResponseVO getAllStockDaily(String date){
        List<TradeRecord> tradeRecordList;
        tradeRecordList = tradeRecordMapper.getAllStockDaily(date);
        List<RatioVO> ratioList = new ArrayList<>();
        for(TradeRecord tradeRecord: tradeRecordList){
            double ratio = (tradeRecord.getClose()-tradeRecord.getOpen())/tradeRecord.getOpen()*100;
            RatioVO ratioVO = new RatioVO(tradeRecord.getCode(),tradeRecord.getClose(),ratio);
            ratioList.add(ratioVO);
        }

        return ResponseVO.buildSuccess(ratioList);
    }
}
