package com.example.matjib.controller;

import com.example.matjib.domain.Member;
import com.example.matjib.domain.PointHistory;
import com.example.matjib.domain.Store;
import com.example.matjib.dto.*;
import com.example.matjib.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final ReviewService reviewService;
    private final StoreService storeService;
    private final MemberService memberService;
    private final PointService pointService;
    private final VisitService visitService;
    private final FileStorageService fileStorageService;
    private final CourseService courseService;

    @org.springframework.beans.factory.annotation.Value("${kakao.js-key:}")
    private String kakaoJsKey;

    // 부산 16개 구/군, 데이터 없어도 필터엔 항상 다 보여줌
    private static final List<String> REGIONS = List.of(
            "부산 중구", "부산 서구", "부산 동구", "부산 영도구", "부산 부산진구",
            "부산 동래구", "부산 남구", "부산 북구", "부산 해운대구", "부산 사하구",
            "부산 금정구", "부산 강서구", "부산 연제구", "부산 수영구", "부산 사상구",
            "부산 기장군");

    // 업로드 용량 초과 시 Spring이 컨트롤러 메서드 진입 전에 예외를 던져서
    // 각 메서드의 try/catch로는 못 잡음 -> 컨트롤러 레벨 핸들러로 잡아서 원래 페이지로 돌려보냄
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(HttpServletRequest request, RedirectAttributes ra) {
        ra.addFlashAttribute("error", "이미지 용량이 너무 커요. 10MB 이하로 올려주세요.");
        Matcher m = Pattern.compile("/stores/(\\d+)").matcher(request.getRequestURI());
        return m.find() ? "redirect:/stores/" + m.group(1) : "redirect:/stores";
    }

    @GetMapping("/")
    public String home(@RequestParam(value = "region", required = false) String region,
                       @RequestParam(value = "page", defaultValue = "1") int page,
                       Model model) {
        List<String> courseRegions = courseService.getRegions();
        String selectedRegion = (region != null && !region.isBlank()) ? region
                : (courseRegions.isEmpty() ? null : courseRegions.get(0));

        StoreSearch listSearch = new StoreSearch();
        listSearch.setPage(page);
        listSearch.setSize(8);
        model.addAttribute("page", storeService.getStores(listSearch));

        if (selectedRegion != null) {
            model.addAttribute("best", storeService.getBestStores(selectedRegion));
            model.addAttribute("course", courseService.getCourse(selectedRegion));
        }

        model.addAttribute("courseRegions", courseRegions);
        model.addAttribute("selectedRegion", selectedRegion);
        return "home";
    }

    @GetMapping("/stores")
    public String storeList(@ModelAttribute StoreSearch search, Model model) {
        PageResponse<StoreListItem> page = storeService.getStores(search);
        model.addAttribute("page", page);
        model.addAttribute("search", search);
        model.addAttribute("regions", REGIONS);
        model.addAttribute("categories", List.of("한식", "양식", "일식", "중식", "카페"));
        return "stores/list";
    }

    @GetMapping("/stores/{storeId}")
    public String storeDetail(@PathVariable Long storeId,
                              @AuthenticationPrincipal UserDetails user,
                              Model model) {
        Store store = storeService.getStore(storeId);
        List<ReviewResponse> reviews = reviewService.getReviewsByStore(storeId);

        Double avgRating = reviews.isEmpty() ? null :
                reviews.stream().mapToInt(ReviewResponse::getRating).average().orElse(0);

        model.addAttribute("store", store);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("rewardPoint", PointService.REVIEW_REWARD);

        boolean visited = false;
        if (user != null) {
            Member member = memberService.getByUsername(user.getUsername());
            visited = visitService.hasVisited(member.getMemberId(), storeId);
        }
        model.addAttribute("visited", visited);
        model.addAttribute("kakaoJsKey", kakaoJsKey);
        return "stores/detail";
    }

    @PostMapping("/stores/{storeId}/visit")
    public String certifyVisit(@PathVariable Long storeId,
                               @AuthenticationPrincipal UserDetails user,
                               RedirectAttributes ra) {
        visitService.certify(storeId, user.getUsername());
        ra.addFlashAttribute("message", "방문 인증이 완료됐어요. 이제 리뷰를 쓸 수 있어요!");
        return "redirect:/stores/" + storeId;
    }

    @PostMapping("/stores/{storeId}/reviews")
    public String createReview(@PathVariable Long storeId,
                               @ModelAttribute ReviewRequest request,
                               @RequestParam(value = "image", required = false) MultipartFile image,
                               @AuthenticationPrincipal UserDetails user,
                               RedirectAttributes ra) {
        try {
            request.setStoreId(storeId);
            String imagePath = fileStorageService.store(image);
            reviewService.create(request, user.getUsername(), imagePath);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stores/" + storeId;
    }

    // /admin/ 경로라 SecurityConfig에서 ROLE_ADMIN을 요구함
    @PostMapping("/admin/stores/{storeId}/image")
    public String uploadStoreImage(@PathVariable Long storeId,
                                   @RequestParam("image") MultipartFile image,
                                   RedirectAttributes ra) {
        try {
            String path = fileStorageService.store(image);
            if (path != null) {
                storeService.updateImage(storeId, path);
                ra.addFlashAttribute("message", "가게 대표사진이 등록됐어요.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stores/" + storeId;
    }

    @PostMapping("/admin/stores/{storeId}/image/delete")
    public String deleteStoreImage(@PathVariable Long storeId, RedirectAttributes ra) {
        try {
            storeService.deleteImage(storeId);
            ra.addFlashAttribute("message", "가게 대표사진을 삭제했어요.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stores/" + storeId;
    }

    @PostMapping("/admin/reviews/{reviewId}/delete")
    public String adminDeleteReview(@PathVariable Long reviewId,
                                    @RequestParam Long storeId,
                                    RedirectAttributes ra) {
        try {
            reviewService.deleteByAdmin(reviewId);
            ra.addFlashAttribute("message", "리뷰가 삭제됐어요.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stores/" + storeId;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signupSubmit(@ModelAttribute SignupRequest request, Model model) {
        try {
            memberService.signup(request);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "signup";
        }
        return "redirect:/login?joined";
    }

    @GetMapping("/mypage")
    public String mypage(@AuthenticationPrincipal UserDetails user, Model model) {
        Member member = memberService.getByUsername(user.getUsername());
        List<PointHistory> history = pointService.getHistory(member.getMemberId());

        int point = member.getPoint() == null ? 0 : member.getPoint();
        model.addAttribute("member", member);
        model.addAttribute("point", point);
        model.addAttribute("history", history);
        model.addAttribute("refundUnit", PointService.REFUND_UNIT);
        model.addAttribute("canRefund", point >= PointService.REFUND_UNIT);
        model.addAttribute("progress", Math.min(100, point * 100 / PointService.REFUND_UNIT));
        return "mypage";
    }

    @PostMapping("/mypage/refund")
    public String refund(@AuthenticationPrincipal UserDetails user,
                         RedirectAttributes ra) {
        Member member = memberService.getByUsername(user.getUsername());
        try {
            pointService.refund(member.getMemberId());
            ra.addFlashAttribute("message", PointService.REFUND_UNIT + "원 환급 신청이 완료되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mypage";
    }
}
