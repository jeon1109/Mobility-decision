package com.example.musinsaPointSystem.data.decision;
import java.util.*;
import com.example.musinsaPointSystem.data.decision.model.*;
/** The rule result owns recommendation identity. AI can only add an explanation. */
public final class RuleRecommendationPolicy {
    private RuleRecommendationPolicy(){}
    public static DecisionRecommendation recommend(List<EvaluatedCandidate> ranked){
        if(ranked==null||ranked.isEmpty())return new DecisionRecommendation(null,
            "선택한 이동 방식의 실제 경로 후보를 확인하지 못했습니다.",List.of(),List.of(),List.of(),null,false,
            List.of("확인된 경로 후보가 없어 경로를 임의로 생성하지 않았습니다."));
        var selected=ranked.getFirst();List<String> reasons=new ArrayList<>(selected.insights());
        if(reasons.isEmpty())reasons.add("확인 가능한 이동 후보를 규칙 기준으로 비교했습니다.");
        reasons.add("경로와 연결된 실시간 근거가 부족하면 사고 없음이나 혼잡 낮음을 단정하지 않습니다.");
        var alternatives=ranked.stream().skip(1).limit(3).map(value->new DecisionRecommendation.Alternative(
            value.candidate().candidateId(),"실제 조회된 다른 이동 후보입니다. 예상시간과 확인 가능한 위험을 비교해주세요.")).toList();
        return new DecisionRecommendation(selected.candidate().candidateId(),
            "확인 가능한 소요시간과 교통 근거를 기준으로 선정한 경로입니다.",reasons,alternatives,selected.risks(),null,false,reasons);
    }
    public static DecisionRecommendation explain(DecisionRecommendation rule,DecisionRecommendation explanation){
        if(rule.recommendedCandidateId()==null||explanation==null||!explanation.aiAvailable()
            ||!rule.recommendedCandidateId().equals(explanation.recommendedCandidateId()))return rule;
        return new DecisionRecommendation(rule.recommendedCandidateId(),explanation.summary(),explanation.reasons(),
            rule.alternatives(),rule.risks(),null,true,rule.ruleReasons());
    }
}
