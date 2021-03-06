package com.boozedb.service;

import com.boozedb.api.model.links.BottleLinks;
import com.boozedb.api.model.links.BottleListLinks;
import com.boozedb.api.model.bottle.BottleListResponse;
import com.boozedb.api.model.bottle.BottleResponse;
import com.boozedb.api.model.bottle.Bottle;
import com.boozedb.core.constants.DefaultParameterValues;
import com.boozedb.core.constants.QueryParamToColumnMap;
import com.boozedb.db.dao.BoozeDbDao;
import com.boozedb.service.views.BoozeDbMeta;
import org.jdbi.v3.core.Jdbi;

import javax.inject.Singleton;
import javax.ws.rs.core.UriInfo;
import java.util.*;

@Singleton
public class BottleService {

    private final BoozeDbDao boozeDbDao;

    Map<String, List<String>> categories = new HashMap<>();

    public BottleService(Jdbi jdbi)
    {
        this.boozeDbDao = new BoozeDbDao(jdbi);
    }

    public Optional<BottleResponse> getBottleById(String id, UriInfo uriInfo)
    {
        Optional<BottleResponse> response;
        Optional<Bottle> bottle = boozeDbDao.getBottleById(id);

        if(bottle.isPresent())
        {
            BottleLinks bottleLinks = bottle.map(value -> new BottleLinks.Builder(value, uriInfo).build()).orElseGet(() -> new BottleLinks.Builder(uriInfo).buildForNotFound());
            //todo make db call to get external links
            response = Optional.of(new BottleResponse.Builder().bottle(bottle.get()).links(bottleLinks).build());
        }
        else
        {
            response = Optional.empty();
        }

        return response;
    }

    public BoozeDbMeta getBoozeDbMetadata()
    {
        BoozeDbMeta result;

        if(categories.size() == 0)
        {
            categories = boozeDbDao.getCategoriesMap();
        }

        result = new BoozeDbMeta(categories);
        return result;
    }

    public BottleListResponse search(UriInfo uriInfo,
                                              Optional<String> category,
                                              Optional<String> subCategory,
                                              Optional<Double> minPrice,
                                              Optional<Double> maxPrice,
                                              Optional<Double> minProof,
                                              Optional<Double> maxProof,
                                              Optional<Double> minSize,
                                              Optional<Double> maxSize,
                                              Optional<Integer> minAge,
                                              Optional<Integer> maxAge,
                                              Optional<Integer> page,
                                              Optional<Integer> pageSize
                                              )
    {

        Map<QueryParamToColumnMap, String> paramMap = getQueryParamMap(category, subCategory, minPrice, maxPrice,minProof, maxProof, minSize, maxSize, minAge, maxAge, page, pageSize);
        List<Bottle> searchResult =  boozeDbDao.search(paramMap);
        BottleListLinks bottleListLinks = new BottleListLinks.Builder(uriInfo).resultSize(searchResult.size()).build();

        return new BottleListResponse.Builder().links(bottleListLinks).results(searchResult).build();
    }

    private Map<QueryParamToColumnMap, String> getQueryParamMap(Optional<String> category, Optional<String> subCategory, Optional<Double> minPrice, Optional<Double> maxPrice, Optional<Double> minProof, Optional<Double> maxProof, Optional<Double> minSize, Optional<Double> maxSize, Optional<Integer> minAge, Optional<Integer> maxAge, Optional<Integer> page, Optional<Integer>  pageSize) {

        Map<QueryParamToColumnMap, String> result = new HashMap<>();

        category.ifPresent(s -> result.put(QueryParamToColumnMap.CATEGORY, s));
        subCategory.ifPresent(s -> result.put(QueryParamToColumnMap.SUB_CATEGORY, s));
        minPrice.ifPresent(aDouble -> result.put(QueryParamToColumnMap.MIN_PRICE, String.valueOf(aDouble)));
        maxPrice.ifPresent(aDouble -> result.put(QueryParamToColumnMap.MAX_PRICE, String.valueOf(aDouble)));
        minProof.ifPresent(aDouble -> result.put(QueryParamToColumnMap.MIN_PROOF, String.valueOf(aDouble)));
        maxProof.ifPresent(aDouble -> result.put(QueryParamToColumnMap.MAX_PROOF, String.valueOf(aDouble)));
        minSize.ifPresent(aDouble -> result.put(QueryParamToColumnMap.MIN_SIZE, String.valueOf(aDouble)));
        maxSize.ifPresent(aDouble -> result.put(QueryParamToColumnMap.MAX_SIZE, String.valueOf(aDouble)));
        minAge.ifPresent(integer -> result.put(QueryParamToColumnMap.MIN_AGE, String.valueOf(integer)));
        maxAge.ifPresent(integer -> result.put(QueryParamToColumnMap.MAX_AGE, String.valueOf(integer)));

        // if page size is present, use its value times page size
        int pageNum = page.orElseGet(() -> DefaultParameterValues.PAGE_OFFSET);
        int limit = pageSize.orElseGet(() -> DefaultParameterValues.PAGE_SIZE);
        int offset = pageNum > 0 ? (pageNum - 1) * limit : 0;

        result.put(QueryParamToColumnMap.PAGE_OFFSET, String.valueOf(offset));
        result.put(QueryParamToColumnMap.PAGE_SIZE, String.valueOf(limit));

        return result;

    }

}
