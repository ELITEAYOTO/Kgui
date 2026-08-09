package me.krunsh.kgui.integration.kfaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumMap;
import java.util.Map;

import org.junit.Test;

import me.krunsh.kfaction.api.v2.ApiResult;
import me.krunsh.kgui.api.ActionResult;

public class KfactionResultMapperTest {

    @Test
    public void everyApiStatusHasAnExplicitStableMapping() {
        Map<ApiResult.Status, ActionResult.Status> expected =
                new EnumMap<ApiResult.Status, ActionResult.Status>(ApiResult.Status.class);
        expected.put(ApiResult.Status.SUCCESS, ActionResult.Status.HANDLED);
        expected.put(ApiResult.Status.NO_CHANGE, ActionResult.Status.HANDLED);
        expected.put(ApiResult.Status.CANCELLED, ActionResult.Status.DENIED);
        expected.put(ApiResult.Status.INVALID_INPUT, ActionResult.Status.DENIED);
        expected.put(ApiResult.Status.NOT_FOUND, ActionResult.Status.DENIED);
        expected.put(ApiResult.Status.FORBIDDEN, ActionResult.Status.DENIED);
        expected.put(ApiResult.Status.CONFLICT, ActionResult.Status.DENIED);
        expected.put(ApiResult.Status.LIMIT_REACHED, ActionResult.Status.DENIED);
        expected.put(ApiResult.Status.UNAVAILABLE, ActionResult.Status.DENIED);
        expected.put(ApiResult.Status.FAILED, ActionResult.Status.ERROR);

        assertEquals(ApiResult.Status.values().length, expected.size());
        for (ApiResult.Status status : ApiResult.Status.values()) {
            ApiResult<Object> source = new ApiResult<Object>(status, null, "source.key", "detail");
            ActionResult mapped = KfactionResultMapper.action(source);
            assertEquals(status.name(), expected.get(status), mapped.getStatus());
            if (status != ApiResult.Status.SUCCESS) {
                assertTrue(status.name(), mapped.getMessageKey() != null);
            }
        }
    }

    @Test
    public void onlyCommittedSuccessRequestsImmediateReread() {
        assertTrue(KfactionResultMapper.action(ApiResult.success("ok")).shouldInvalidate());
        ActionResult noChange = KfactionResultMapper.action(new ApiResult<Object>(
                ApiResult.Status.NO_CHANGE, null, "same", null));
        assertFalse(noChange.shouldInvalidate());
    }
}
