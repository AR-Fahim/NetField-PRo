AMENDMENT 1 (supersedes M7a pinned decisions where they conflict)
1. Persisted SurveySession.state stays DRAFT|FINALIZED. During a survey it is DRAFT.
2. SM-1 states (CREATED..ENDED) are runtime-only. They live in SessionRuntimeStore:
   SharedPreferences file "nf_session_runtime" (excluded from backup),
   key = sessionId, value = JSON {"s":"<Sm1State>","a":<lastActivityNs>,"g":[["SCREEN_OFF",<tNs>],...]}.
   g is capped at 500 entries (oldest dropped). A missing entry is equivalent to ENDED.
3. Effect PERSIST_STATE = store.putState. Effect LOG_GAP = store.appendGap.
4. ConnectionEvent rows are only CONNECT|DISCONNECT|ROAM (written by M16). Gaps never go to Room.
5. Gaps are NOT part of signed evidence in the prototype. Schema v2 will add them (post-prototype).
6. The reboot rule (createdAtMs vs monoEpochNs) still reads the session row. Unchanged.