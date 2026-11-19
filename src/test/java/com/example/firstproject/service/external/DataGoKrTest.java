package com.example.firstproject.service.external;

import com.example.firstproject.common.Nums;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DataGoKrTest {

    @Test
    void encodeKey_decodingKey_isEncodedOnce() {
        assertThat(DataGoKr.encodeKey("ab+c/d=="))
                .isEqualTo("ab%2Bc%2Fd%3D%3D");
    }

    @Test
    void encodeKey_alreadyEncodedKey_isKeptAsIs() {
        assertThat(DataGoKr.encodeKey("ab%2Bc%2Fd%3D%3D"))
                .isEqualTo("ab%2Bc%2Fd%3D%3D");
    }

    @Test
    void describeError_jsonGatewayError() {
        String body = """
                {"OpenAPI_ServiceResponse":{"cmmMsgHeader":{
                  "errMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR",
                  "returnAuthMsg":"등록되지 않은 서비스키","returnReasonCode":"30"}}}""";
        assertThat(DataGoKr.isGatewayError(body)).isTrue();
        assertThat(DataGoKr.describeError(body))
                .startsWith("등록되지 않은 서비스키 [SERVICE_KEY_IS_NOT_REGISTERED_ERROR]")
                .contains("활용신청");
    }

    @Test
    void describeError_xmlGatewayError() {
        String body = """
                <OpenAPI_ServiceResponse><cmmMsgHeader>
                  <errMsg>LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR</errMsg>
                  <returnAuthMsg>LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR</returnAuthMsg>
                </cmmMsgHeader></OpenAPI_ServiceResponse>""";
        assertThat(DataGoKr.describeError(body)).contains("호출 한도");
    }

    @Test
    void nums_parse_handlesCommasAndBlanks() {
        assertThat(Nums.parse("  82,500 ")).isEqualByComparingTo(new BigDecimal("82500"));
        assertThat(Nums.parse("1,385.5")).isEqualByComparingTo(new BigDecimal("1385.5"));
        assertThat(Nums.parse("")).isNull();
        assertThat(Nums.parse(null)).isNull();
        assertThat(Nums.parse("abc")).isNull();
    }
}
