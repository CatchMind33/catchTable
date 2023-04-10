package com.catchmind.catchtable.controller;

import com.catchmind.catchtable.domain.BistroDetail;
import com.catchmind.catchtable.domain.Profile;
import com.catchmind.catchtable.dto.PendingDto;
import com.catchmind.catchtable.dto.network.request.ProfileRequest;
import com.catchmind.catchtable.dto.network.response.IndexResponse;
import com.catchmind.catchtable.service.MainService;
import com.catchmind.catchtable.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Controller
@RequestMapping("")
public class PageController {
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;
    private final MainService mainService;

    public Integer getWeekNumber (LocalDate date) {
        LocalDate firstMondayOfMonth = date.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));

        // 첫 월요일이면 바로 리턴
        if (firstMondayOfMonth.isEqual(date)) return 1;

        if (date.isAfter(firstMondayOfMonth)) {
            // 첫 월요일 이후일 때
            int diffFromFirstMonday = date.getDayOfMonth() - firstMondayOfMonth.getDayOfMonth();
            int weekNumber = (int) Math.ceil(diffFromFirstMonday / 7.0);
            if (date.getDayOfWeek() == DayOfWeek.MONDAY) weekNumber += 1;
            return weekNumber;
        }
        // 첫 월요일 이전이면 회귀식으로 전 달 마지막 주차를 구함
        return getWeekNumber(date.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()));
    }

    @GetMapping("")
    public String index(Model model) {
        LocalDate date = LocalDate.now();;
        Integer week = getWeekNumber(date);
        List<IndexResponse> list = mainService.indexReviewList();
        List<BistroDetail> bisList = mainService.indexList();
        model.addAttribute("list",list);
        model.addAttribute("bisList",bisList);
        model.addAttribute("week",week);
        return "index";
    }

    @GetMapping("/login")
    public ModelAndView login() {
        return new ModelAndView("login");
    }

    @GetMapping("/login/error")
    public ModelAndView loginFail() {
        return new ModelAndView("loginFail");
    }

    @GetMapping("join")
    public ModelAndView join() {
        return new ModelAndView("join");
    }

    @PostMapping("/join")
    public String join(ProfileRequest request, BindingResult bindingResult, Model model){
        if (bindingResult.hasErrors()){
            return "join";
        }
        try{
            Profile profile = Profile.createMember(request, passwordEncoder);
            profileService.saveMember(profile);
        }catch (IllegalStateException e){
            model.addAttribute("errorMessage",e.getMessage());
            return "join";
        }
        return "redirect:/";
    }

    @PostMapping("/idCheck")
    @ResponseBody
    public boolean nickCheck(@RequestParam("prNick")String prNick){
        Optional<Profile> profile = profileService.checkNick(prNick);
        if (profile.isEmpty()){
            return true;
        }else {
            return false;
        }
    }

    // 입점문의 페이지
    @GetMapping("pending")
    public ModelAndView inquiry (){
        return new ModelAndView("inquiry");
    }

    // 입점문의 등록
    @PostMapping("/pending")
    public String inquiry (PendingDto pendingDto){
        System.out.println(pendingDto);
        mainService.createResAdmin(pendingDto);
        return "redirect:/";
    }

    @GetMapping("/findPassword")
    public ModelAndView findPw () {
        return new ModelAndView("findPw");
    }

    @PostMapping("/findPassword")
    @ResponseBody
    public Optional<Profile> findPassword (@RequestParam("prHp")String prHp,
                                           @RequestParam("prName")String prName) {
        Optional<Profile> profile = profileService.findPw(prHp,prName);
        return profile;
    }
    @GetMapping("/resetPassword/{prHp}")
    public ModelAndView resetPw (@PathVariable String prHp, Model model){
        model.addAttribute("prHp",prHp);
        return new ModelAndView("/resetPassword");
    }
    @PostMapping("/resetPassword")
    public String resetPassword (@RequestParam("prHp")String prHp,
                                 ProfileRequest request){
        System.out.println("🐓🐓🐓🐓🐓🐓🐓🐓🐓  "+request.prHp());
        profileService.updatePassword(prHp, request.toDto());
        return "login";
    }


}
