package com.example.firstproject.service;

import com.example.firstproject.dto.SubscriptionNoticeDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 응답 해석/합치기 로직 테스트. 샘플 JSON 은 공개 명세의 필드 이름을 따른 것이라
 * 실제 키 승인 후 응답과 다르면 여기부터 맞추면 됩니다.
 */
class SubscriptionServiceTest {

    private final ObjectMapper om = new ObjectMapper();
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

    @Test
    void parseApplyhome_mapsFieldsAndStatus() throws Exception {
        JsonNode root = om.readTree("""
                {"currentCount":2,"matchCount":2,"page":1,"perPage":500,"totalCount":9000,"data":[
                  {"HOUSE_MANAGE_NO":"2026000123","PBLANC_NO":"2026000123","HOUSE_NM":"래미안 테스트",
                   "HOUSE_SECD_NM":"APT","HOUSE_DTL_SECD_NM":"민영","SUBSCRPT_AREA_CODE_NM":"서울",
                   "HSSPLY_ADRES":"서울특별시 강남구 어딘가 1","TOT_SUPLY_HSHLDCO":"312",
                   "RCRIT_PBLANC_DE":"2026-11-14","RCEPT_BGNDE":"2026-11-24","RCEPT_ENDDE":"2026-11-26",
                   "PRZWNER_PRESNATN_DE":"2026-12-03","PBLANC_URL":"https://www.applyhome.co.kr/x"},
                  {"HOUSE_MANAGE_NO":"2026000100","HOUSE_NM":"지난 단지","SUBSCRPT_AREA_CODE_NM":"경기",
                   "RCRIT_PBLANC_DE":"2026-10-01","RCEPT_BGNDE":"2026-10-10","RCEPT_ENDDE":"2026-10-12"}
                ]}""");

        List<SubscriptionNoticeDto> list = SubscriptionService.parseApplyhome(root, TODAY);

        assertThat(list).hasSize(2);
        SubscriptionNoticeDto a = list.get(0);
        assertThat(a.source()).isEqualTo("APPLYHOME");
        assertThat(a.name()).isEqualTo("래미안 테스트");
        assertThat(a.category()).isEqualTo("민영");
        assertThat(a.region()).isEqualTo("서울");
        assertThat(a.households()).isEqualTo(312);
        assertThat(a.winnerDate()).isEqualTo("2026-12-03");
        assertThat(a.status()).isEqualTo("접수중");
        assertThat(list.get(1).status()).isEqualTo("마감");
        assertThat(list.get(1).category()).isNull();
    }

    @Test
    void parseLh_findsDsListAnywhereAndSkipsLand() throws Exception {
        JsonNode root = om.readTree("""
                [{"dsSch":[{"PAN_ST_DT":"20260925"}]},
                 {"resHeader":[{"SS_CODE":"Y"}],"dsList":[
                   {"PAN_ID":"0000060001","PAN_NM":"[정정공고] 화성동탄 행복주택","UPP_AIS_TP_NM":"임대주택",
                    "AIS_TP_CD_NM":"행복주택","CNP_CD_NM":"경기도","PAN_NT_ST_DT":"2026.11.20",
                    "CLSG_DT":"2026.12.05","PAN_SS":"접수중","DTL_URL":"https://apply.lh.or.kr/x","ALL_CNT":"3"},
                   {"PAN_ID":"0000060002","PAN_NM":"세종 단독주택용지","UPP_AIS_TP_NM":"토지",
                    "CNP_CD_NM":"세종특별자치시","PAN_NT_ST_DT":"2026.11.18","CLSG_DT":"2026.11.30","PAN_SS":"공고중"},
                   {"PAN_ID":"0000060004","PAN_NM":"울산 관리동 가정어린이집 운영자 모집","UPP_AIS_TP_NM":"임대주택",
                    "AIS_TP_CD_NM":"가정어린이집","CNP_CD_NM":"울산광역시","PAN_NT_ST_DT":"2026.11.18","PAN_SS":"공고중"},
                   {"PAN_ID":"0000060003","PAN_NM":"서울 신혼희망타운","UPP_AIS_TP_NM":"분양주택",
                    "CNP_CD_NM":"서울특별시","PAN_NT_ST_DT":"2026.11.01","CLSG_DT":"2026.11.10"}
                 ]}]""");

        List<SubscriptionNoticeDto> list = SubscriptionService.parseLh(root, TODAY);

        assertThat(list).extracting(SubscriptionNoticeDto::id).containsExactly("0000060001", "0000060003");
        SubscriptionNoticeDto a = list.get(0);
        assertThat(a.category()).isEqualTo("행복주택");
        assertThat(a.region()).isEqualTo("경기");
        assertThat(a.noticeDate()).isEqualTo("2026-11-20");
        assertThat(a.receiptEnd()).isEqualTo("2026-12-05");
        assertThat(a.status()).isEqualTo("접수중");
        // 상태가 없으면 날짜로 계산
        assertThat(list.get(1).status()).isEqualTo("마감");
        assertThat(list.get(1).region()).isEqualTo("서울");
    }

    @Test
    void merge_dedupsSameNoticeAndSortsByStatus() {
        SubscriptionNoticeDto closed = dto("APPLYHOME", "A", "2026-10-01", "마감");
        SubscriptionNoticeDto open = dto("APPLYHOME", "B 단지", "2026-11-20", "접수중");
        SubscriptionNoticeDto upcoming = dto("LH", "C", "2026-11-22", "접수예정");
        SubscriptionNoticeDto lhDup = dto("LH", "B단지", "2026-11-20", "접수중");

        List<SubscriptionNoticeDto> merged = SubscriptionService.merge(List.of(closed, open), List.of(upcoming, lhDup));

        assertThat(merged).containsExactly(open, upcoming, closed);
    }

    @Test
    void helpers() {
        assertThat(SubscriptionService.date("2026.01.05")).isEqualTo("2026-01-05");
        assertThat(SubscriptionService.date("20260105")).isEqualTo("2026-01-05");
        assertThat(SubscriptionService.date("")).isNull();
        assertThat(SubscriptionService.shortRegion("전라남도")).isEqualTo("전남");
        assertThat(SubscriptionService.shortRegion("부산광역시")).isEqualTo("부산");
        assertThat(SubscriptionService.shortRegion("전북특별자치도")).isEqualTo("전북");
        assertThat(SubscriptionService.shortRegion("전남광주통합특별시")).isEqualTo("광주·전남");
        assertThat(SubscriptionService.shortRegion("대구광역시 외")).isEqualTo("대구 외");
        assertThat(SubscriptionService.shortRegion("전국")).isEqualTo("전국");
        assertThat(SubscriptionService.statusOf("2026-12-01", "2026-12-03", TODAY)).isEqualTo("접수예정");
    }

    private static SubscriptionNoticeDto dto(String src, String name, String noticeDate, String status) {
        return new SubscriptionNoticeDto(src, name, name, null, null, null, null, noticeDate, null, null, null, status, null);
    }
}
