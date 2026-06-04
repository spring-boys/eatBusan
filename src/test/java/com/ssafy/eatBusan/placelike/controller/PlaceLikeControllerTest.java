package com.ssafy.eatBusan.placelike.controller;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.eatBusan.auth.domain.TokenType;
import com.ssafy.eatBusan.auth.util.JWTUtil;
import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.place.Repository.PlaceRepository;
import com.ssafy.eatBusan.place.domain.Place;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeDetailResponseDto;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeRequestDto;
import com.ssafy.eatBusan.placelike.mapper.PlaceLikeMapper;
import com.ssafy.eatBusan.util.TestUtil;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class PlaceLikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PlaceLikeMapper placeLikeMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private TestUtil testUtil;

    @Test
    @DisplayName("장소_좋아요_누르기_성공")
    void createNewPlaceLike() throws Exception {

        //given
        Member member = testUtil.createTestMember();
        memberRepository.save(member);

        Place place = testUtil.createTestPlace();
        placeRepository.save(place);

        String token = jwtUtil.createToken(member, TokenType.ACCESS);

        //when-then
        mockMvc.perform(post("/api/places/{placeId}/likes", place.getId())
                        .header("Authorization", String.format("Bearer %s", token)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("장소_좋아요_누르기_실패_중복_불가")
    void createNewPlaceLikeFail() throws Exception {

        //given
        Member member = testUtil.createTestMember();
        memberRepository.save(member);

        Place place = testUtil.createTestPlace();
        placeRepository.save(place);

        placeLikeMapper.insertPlaceLike(new PlaceLikeRequestDto(member.getId(), place.getId()));

        String token = jwtUtil.createToken(member, TokenType.ACCESS);

        //when-then
        mockMvc.perform(post("/api/places/{placeId}/likes", place.getId())
                        .header("Authorization", String.format("Bearer %s", token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("나의_좋아요_목록을_조회")
    void getMyLikeList() throws Exception {

        //given
        Member member = testUtil.createTestMember();
        memberRepository.save(member);

        Place place1 = testUtil.createTestPlace();
        Place place2 = testUtil.createTestPlace2();
        placeRepository.save(place1);
        placeRepository.save(place2);

        String token = jwtUtil.createToken(member, TokenType.ACCESS);
        placeLikeMapper.insertPlaceLike(new PlaceLikeRequestDto(member.getId(), place1.getId()));
        placeLikeMapper.insertPlaceLike(new PlaceLikeRequestDto(member.getId(), place2.getId()));

        //when-then
        MvcResult result = mockMvc.perform(get("/api/places/likes/my")
                        .header("Authorization", String.format("Bearer %s", token)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        List<PlaceLikeDetailResponseDto> list = objectMapper.readValue(
                content,
                objectMapper.getTypeFactory().constructCollectionType(List.class, PlaceLikeDetailResponseDto.class)
        );

        assertSoftly(softly -> {
            softly.assertThat(list.size()).isEqualTo(2);
            softly.assertThat(list.getFirst().name()).isEqualTo(place1.getName()); //오름차순 정렬중
            softly.assertThat(list.getLast().phone()).isEqualTo(place2.getPhone());
        });

    }
}