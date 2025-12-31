package disruptor.practice.common;

import disruptor.practice.oms.model.FamilyState;

@FunctionalInterface
public interface ReleaseHook {

    void onAboutToRelease(int family, FamilyState familyState);
}
