package com.github.phantomthief.test;

import static java.util.function.Function.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.github.phantomthief.util.IMultiDataAccess;
import com.github.phantomthief.util.RetrieveIdUtils;
import com.google.common.collect.ImmutableList;

/**
 * @author w.vela
 * Created on 16/5/10.
 */
public class TestIMultiDataBuilder {

    @Test
    public void test() {
        List<Integer> list = new ArrayList<>();
        RetrieveIdUtils
                .get(list,
                        ImmutableList
                                .of(IMultiDataAccess
                                        .<Integer, Long> get(keys -> keys.stream()
                                                .collect(Collectors.toMap(identity(),
                                                        Integer::longValue)))
                                        .set(System.out::println)));
    }
}
