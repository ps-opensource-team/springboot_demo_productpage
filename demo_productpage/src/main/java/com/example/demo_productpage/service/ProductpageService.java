package com.example.demo_productpage.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.demo_productpage.dao.ProductpageDAO;
import com.example.demo_productpage.dto.ProductpageDTO;
import com.example.demo_productpage.repository.ProductpageRepository;
import com.example.demo_productpage.util.CommUtil;
import com.example.demo_productpage.util.ServiceUtil;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

// import io.opentracing.Tracer;

/**
 * @className ProductpageService
 * @mehtod getProductpageInfo, getDetailsInfo, postReviewsInfo
 * @description 아래 예제는 Employee 정보를 관리하는 서비스 Service 입니다.
 */
@Service
public class ProductpageService {

    Logger logger = LoggerFactory.getLogger(ProductpageService.class);

    @Autowired
    ProductpageRepository repository;

    // @Autowired
    // private Tracer tracer; 

    @Autowired
    private Environment env;
    
    @Autowired
    public RestTemplate restTemplate;

    // @Autowired
    // public ServiceUtil(RestTemplate restTemplate) {
    //     this.restTemplate = restTemplate;
    // }

    /**
     * @methodName  getProductpageInfo
     * @return      ProductpageDTO.MainResponse
     * @throws      Exception
     * @description Productpage Main 정보를 가져오는 Mehtod 입니다.
     */
    public ProductpageDTO.MainResponse getProductpageInfo() throws Exception {

        ProductpageDTO.MainResponse response = new ProductpageDTO.MainResponse();
        List<ProductpageDTO.Product> prodList = new ArrayList<ProductpageDTO.Product>();

        try {
            // ProductpageRepository를 사용하여 productpage table의 모든 데이터를 조회
            List<ProductpageDAO> daoList = repository.findByProdCode();
            // System.out.println("                      daoList : " + daoList);
            
            for(ProductpageDAO dao : daoList) {
                ProductpageDTO.Product product = new ProductpageDTO.Product();
                product.setProdCode(dao.getProdCode());
                product.setProdName(dao.getProdName());
                product.setProdIntro(dao.getProdIntro());

                // File을 String으로 변환하여 product에 setting
                // 테스트를 위해서 잠시 막아둠
                File file = new File(dao.getImgPath());
                product.setProdImg(CommUtil.fileToString(file));
                prodList.add(product);
            }
            response.setProductList(prodList);
            response.setResultCode(200);
        } catch(Exception e) {
            e.printStackTrace();
            response.setResultCode(500);
        }

        return response;
    }

    /**
     * @methodName  getDetailsInfo
     * @return      ProductpageDTO.DetailsResponse
     * @throws      Exception
     * @description Detail 화면의 Product Detail 정보와 Product의 Review, Rating 정보들을 가져오는 Method 입니다.
     */
    public ProductpageDTO.DetailsResponse getDetailsInfo(HttpHeaders requestHeader, String prodCode) throws Exception {

        ProductpageDTO.DetailsResponse response = new ProductpageDTO.DetailsResponse();

        // test
        ServiceUtil serviceUtil = new ServiceUtil();

        // Remote Service Call을 위한 Setting
        String baseDetailsURL = ServiceUtil.DETAILS_URI + "/" + ServiceUtil.DETAILS_SERVICE + "?prodCode=" + prodCode;
        String baseReviewsURL = ServiceUtil.REVIEWS_URI + "/" + ServiceUtil.REVIEWS_SERVICE + "?prodCode=" + prodCode;
        final HttpEntity<String> httpEntity = new HttpEntity<String>(requestHeader);
        String httpMethod = "GET";
        boolean flag = false;

        try {
            // Remote Service Call(Details Service)
            JSONObject detailsResponse = serviceUtil.callRemoteService(restTemplate, baseDetailsURL, httpEntity, httpMethod);
            // System.out.println("                         detailsResponse : " + detailsResponse);

            response.setProdCode(prodCode);
            response.setProdName((String)detailsResponse.get("prodName"));
            response.setDetailImg((String)detailsResponse.get("detailsImg"));

            if("200".equals(String.valueOf(detailsResponse.get("resultCode")))) flag = true;

            // Remote Service Call(Reviews Service)
            JSONObject reviewsResponse = serviceUtil.callRemoteService(restTemplate, baseReviewsURL, httpEntity, httpMethod);
            // System.out.println("                         reviewsResponse : " + reviewsResponse);

            // // reviewsResponse에서 reviewList 추출
            JSONArray reviewsList = (JSONArray)reviewsResponse.get("reviewsList");
            List<ProductpageDTO.Review> tempList = new ArrayList<>();
            for(Object reviews : reviewsList) {
                JSONObject tempObject = (JSONObject)reviews;

                
                ProductpageDTO.Review review = new ProductpageDTO.Review();
                review.setReviewsId((String)tempObject.get("reviewsId"));
                review.setContents((String)tempObject.get("contents"));
                if(tempObject.get("rating") != null) review.setRating(Integer.valueOf(String.valueOf(tempObject.get("rating"))));
                tempList.add(review);
            }

            if("200".equals(String.valueOf(detailsResponse.get("resultCode")))) flag = true;

            if(flag) response.setResultCode(200);

            response.setReviewList(tempList);
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }

        return response;
    }

    /**
     * @methodName  postReviewsInfo
     * @return      ProductpageDTO.reviewsResponse
     * @throws      Exception
     * @description Review와 Rating 정보를 입력 받아 입력하는 서비스를 호출하는 Mehtod 입니다.
     */
    public ProductpageDTO.reviewsResponse postReviewsInfo(HttpHeaders requestHeader, ProductpageDTO.Request request) throws Exception {

        ProductpageDTO.reviewsResponse response = new ProductpageDTO.reviewsResponse();

        // test
        ServiceUtil serviceUtil = new ServiceUtil();

        // Remote Service Call을 위한 Setting
        String baseURL = ServiceUtil.REVIEWS_URI + "/" + ServiceUtil.REVIEWS_SERVICE;
        Map<String, Object> req_payload = new HashMap<>();
        req_payload.put("cudFlag", request.getCudFlag());
        req_payload.put("prodCode", request.getProdCode());
        req_payload.put("reviewsId", request.getReviewsId());
        req_payload.put("contents", request.getContents());
        req_payload.put("rating", request.getRating());
        final HttpEntity<?> httpEntity = new HttpEntity<>(req_payload, requestHeader);
        String httpMethod = "POST";

        try {
            // Remote Service Call
            JSONObject callResponse = serviceUtil.callRemoteService(restTemplate, baseURL, httpEntity, httpMethod);

            response.setResultCode(Integer.valueOf(String.valueOf(callResponse.get("resultCode"))));

        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }

        return response;
    }
}
