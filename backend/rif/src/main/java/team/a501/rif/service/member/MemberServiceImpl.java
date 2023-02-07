package team.a501.rif.service.member;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import team.a501.rif.config.Jwt.JwtAuthenticationFilter;
import team.a501.rif.config.Jwt.JwtTokenProvider;
import team.a501.rif.domain.achievement.AchievementAcq;
import team.a501.rif.domain.badge.Badge;
import team.a501.rif.domain.badge.BadgeAcq;
import team.a501.rif.domain.member.Member;
import team.a501.rif.dto.achievement.AchievementAcqInfo;
import team.a501.rif.dto.badge.BadgeAcqInfo;
import team.a501.rif.dto.member.*;
import team.a501.rif.dto.riflog.RifLogInfo;
import team.a501.rif.dto.riflog.RifLogSaveRequest;
import team.a501.rif.exception.ExceptionCode;
import team.a501.rif.exception.RifCustomException;
import team.a501.rif.repository.achievement.AchievementRepository;
import team.a501.rif.repository.badge.BadgeRepository;
import team.a501.rif.repository.member.MemberRepository;
import team.a501.rif.service.achievement.AchievementAcqService;
import team.a501.rif.service.badge.BadgeAcqService;
import team.a501.rif.service.badge.BadgeService;
import team.a501.rif.service.riflog.RifLogService;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final BadgeService badgeService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtTokenProvider jwtTokenProvider;
    private final BadgeRepository badgeRepository;
    private final BadgeAcqService badgeAcqService;
    private final AchievementRepository achievementRepository;
    private final AchievementAcqService achievementAcqService;
    private final RifLogService rifLogService;


    @Override
    public MemberResponse register(MemberRegisterRequest dto) {
        Member member = memberRepository.save(Member.builder()
                .id(dto.getId())
                .password(passwordEncoder.encode(dto.getPassword()))
                .uid(dto.getUid())
                .name(dto.getName())
                .point(1000)
                .exp(0)
                .profileImgPath(Member.DEFAULT_PROFILE_IMG)
                .build());

        return MemberResponse.builder()
                .id(member.getId())
                .uid(member.getUid())
                .name(member.getName())
                .imgPath(member.getProfileImgPath())
                .build();
    }

    @Override
    public void registerAll(List<MemberRegisterRequest> dtoList) {
        for (var e : dtoList) {
            memberRepository.save(Member.builder()
                    .id(e.getId())
                    .password(passwordEncoder.encode(e.getPassword()))
                    .uid(e.getUid())
                    .name(e.getName())
                    .point(1000)
                    .exp(0)
                    .profileImgPath(Member.DEFAULT_PROFILE_IMG)
                    .build());
            memberRepository.flush();
        }
    }

    @Override
    public MemberResponse findByUid(String uid) {
        Member member = memberRepository
                .findByUid(uid)
                .orElseThrow(() -> new RifCustomException(ExceptionCode.ENTITY_INSTANCE_NOT_FOUND));

        return MemberResponse.builder()
                .id(member.getId())
                .uid(member.getUid())
                .name(member.getName())
                .imgPath(member.getProfileImgPath())
                .build();
    }

    @Override
    public MemberResponse findById(String id) {
        Member member = memberRepository
                .findById(id)
                .orElseThrow(() -> new RifCustomException(ExceptionCode.ENTITY_INSTANCE_NOT_FOUND));

        return MemberResponse.builder()
                .id(member.getId())
                .uid(member.getUid())
                .name(member.getName())
                .imgPath(member.getProfileImgPath())
                .build();
    }

    @Override
    public List<BadgeAcqInfo> findAllBadgeAcq(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RifCustomException(ExceptionCode.ENTITY_INSTANCE_NOT_FOUND));

        List<BadgeAcqInfo> badgeAcqInfoList = member
                .getBadgeAcqs()
                .values()
                .stream()
                .map(badgeAcq -> BadgeAcqInfo.from(badgeAcq))
                .collect(Collectors.toList());

        return badgeAcqInfoList;
    }

    @Override
    public List<BadgeAcqInfo> findBadgeAcqOnDisplay(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RifCustomException(ExceptionCode.ENTITY_INSTANCE_NOT_FOUND));

        List<BadgeAcqInfo> badgeAcqInfoList = member
                .getBadgeAcqs()
                .values()
                .stream()
                .filter(acq -> acq.getOnDisplay())
                .map(acq -> BadgeAcqInfo.from(acq))
                .collect(Collectors.toList());

        return badgeAcqInfoList;
    }

    @Override
    @Transactional
    public List<AchievementAcqInfo> findAllAchievementAcq(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RifCustomException(ExceptionCode.ENTITY_INSTANCE_NOT_FOUND));

        List<AchievementAcqInfo> achievementAcqInfoList = member
                .getAchievementAcqs()
                .values()
                .stream()
                .map(acq -> AchievementAcqInfo.from(acq))
                .collect(Collectors.toList());

        return achievementAcqInfoList;
    }

    @Override
    public List<AchievementAcqInfo> findAchievementAcqOnDisplay(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RifCustomException(ExceptionCode.ENTITY_INSTANCE_NOT_FOUND));

        List<AchievementAcqInfo> achievementAcqInfoList = member.getAchievementAcqs()
                .values()
                .stream()
                .filter(acq -> acq.getOnDisplay())
                .map(acq -> AchievementAcqInfo.from(acq))
                .collect(Collectors.toList());

        return achievementAcqInfoList;
    }

    private static final Integer GATCHA_COST = 100;

    @Override
    public BadgeGatchaResponse drawRandomBadge(String memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow();

        Integer balance = member.getPoint();
        if (balance < GATCHA_COST)
            throw new RifCustomException(ExceptionCode.NOT_ENOUGH_POINTS);

        member.setPoint(balance - GATCHA_COST);

        // get random badge
        Badge badge = badgeService.getRandomBadge();

        // if we already have the badge
        Boolean reduplicated = member.hasBadge(badge.getId());

        // or add badgeAcq to member
        if (!reduplicated) {
            badgeAcqService.save(memberId, badge.getId());
        }

        return BadgeGatchaResponse.builder()
                .reduplicated(reduplicated)
                .remainingPoint(member.getPoint())
                .badge(badge.getInfo())
                .build();
    }

    @Override
    public BadgeAcqInfo updateBadgeOnDisplay(String memberId, Long badgeId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RifCustomException(ExceptionCode.ENTITY_INSTANCE_NOT_FOUND));

        BadgeAcq badgeAcq = Optional.of(member.getBadgeAcqs().get(badgeId))
                .orElseThrow(() -> new RifCustomException(ExceptionCode.ENTITY_INSTANCE_NOT_FOUND));

        badgeAcq.toggleOnDisplay();

        return badgeAcq.getInfo();
    }

    @Override
    public AchievementAcqInfo updateAchievementOnDisplay(String memberId, Long achievementId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RifCustomException(ExceptionCode.ENTITY_INSTANCE_NOT_FOUND));

        AchievementAcq achievementAcq = Optional.of(member.getAchievementAcqs().get(achievementId))
                .orElseThrow(() -> new RifCustomException(ExceptionCode.ENTITY_INSTANCE_NOT_FOUND));

        achievementAcq.toggleOnDisplay();

        return achievementAcq.getInfo();
    }

    @Override
    public BadgeAcqInfo addBadgeAcq(String memberId, Long badgeId) {

        return badgeAcqService.save(memberId, badgeId);
    }

    @Override
    public AchievementAcqInfo addAchievementAcq(String memberId, Long achievementId) {
        return achievementAcqService.save(memberId, achievementId);
    }

    @Override
    public RifLogInfo addRifLog(RifLogSaveRequest dto) {
        RifLogInfo rifLogInfo = rifLogService.save(dto);
        return rifLogInfo;
    }

    @Override
    public void deleteByUid(String uid) {

        Member member = memberRepository
                .findByUid(uid)
                .orElseThrow(() -> new RifCustomException(ExceptionCode.ENTITY_INSTANCE_NOT_FOUND));

        // Badge.badgeAcqs에서 삭제
        for (var acq : member.getBadgeAcqs().values()) {
            acq.getBadge().removeBadgeAcq(acq);
        }

        // Achievement.achievementAcqs에서 삭제
        for (var acq : member.getAchievementAcqs().values()) {
            acq.getAchievement().removeAchievementAcq(acq);
        }

        memberRepository.delete(member);
    }

    @Override
    public void deleteById(String id) {

        Member member = memberRepository
                .findById(id)
                .orElseThrow(() -> new RifCustomException(ExceptionCode.ENTITY_INSTANCE_NOT_FOUND));

        for (var acq : member.getBadgeAcqs().values()) {
            acq.getBadge().removeBadgeAcq(acq);
        }

        for (var acq : member.getAchievementAcqs().values()) {
            acq.getAchievement().removeAchievementAcq(acq);
        }

        memberRepository.delete(member);

    }

    @Override
    public MemberResponse passwordChange(HttpServletRequest request, String memberId, PasswordChangeRequest passwordChangeRequest) {
        String accessToken = jwtAuthenticationFilter.resolveToken(request);

        log.info("passwordChange info : {}", passwordChangeRequest, memberId);
        log.info("memberid ={}", memberId);
        log.info("accesstoken info ={}", accessToken);
        Claims claims = jwtTokenProvider.parseClaims(accessToken);
        log.info("Claims info = {}", claims);
        Member member = memberRepository.findById(claims.getSubject()).orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을수 없습니다."));
        log.info("Member by token info = {}", member);
        if (!memberId.equals(member.getId())) throw new BadCredentialsException("잘못된 유저입니다.");
        if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), member.getPassword()))
            throw new BadCredentialsException("다시 입력해주세요.");
        if (!passwordChangeRequest.getNewPassword().equals(passwordChangeRequest.getNewPasswordConfirm()))
            throw new BadCredentialsException("다시 입력해주세요.");
        Member changeMember = memberRepository.save(Member.builder()
                .id(member.getId())
                .uid(member.getUid())
                .exp(member.getExp())
                .password(passwordEncoder.encode(passwordChangeRequest.getNewPassword()))
                .name(member.getName())
                .point(member.getPoint())
                .profileImgPath(member.getProfileImgPath())
                .build());
        return MemberResponse.builder()
                .id(changeMember.getId())
                .uid(changeMember.getUid())
                .name(changeMember.getName())
                .imgPath(changeMember.getProfileImgPath())
                .build();
    }

    @Override
    public List<GetMembersName> getMembersName() {
        List<Member> getNameAll = memberRepository.findAll();
        List<GetMembersName> response = new ArrayList<>();
        for (Member b : getNameAll) {
            response.add(GetMembersName.builder().name(b.getName()).build());
        }
        return response;
    }

    @Override
    public List<FindMemberByName> findByName(String name) {
        List<Member> repo = memberRepository.findAllByName(name);
        List<FindMemberByName> response = new ArrayList<>();
        for (Member b : repo) {
            response.add(FindMemberByName.builder()
                    .id(b.getId())
                    .name(b.getName())
                    .exp(b.getExp())
                    .profileImgPath(b.getProfileImgPath())
                    .build());
        }
        return response;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) {

        UserDetails userDetails = memberRepository
                .findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("해당하는 username 으로 멤버를 조회할 수 없습니다"));

        return userDetails;
    }
}