-- 업체 데이터 샘플 5개 SUPPLIER (UUID 자동 생성)
INSERT INTO p_company (id, created_at, created_by, deleted, deleted_at, deleted_by, updated_at, updated_by, name, type, hub_id, address)
VALUES (gen_random_uuid(), NOW(), 'master1', false, NULL, NULL, NOW(), 'master1', 'A 서울물류업체', 'SUPPLIER', 'd0c14c9e-08f7-46c2-a4a6-c79abfa58f56', '서울특별시 송파구 송파대로 100');

INSERT INTO p_company (id, created_at, created_by, deleted, deleted_at, deleted_by, updated_at, updated_by, name, type, hub_id, address)
VALUES (gen_random_uuid(), NOW(), 'master1', false, NULL, NULL, NOW(), 'master1', 'B 경기북부물류업체', 'SUPPLIER', '96eb63b7-3317-4400-96f0-4424d9949b6c', '경기도 고양시 덕양구 권율대로 500');

INSERT INTO p_company (id, created_at, created_by, deleted, deleted_at, deleted_by, updated_at, updated_by, name, type, hub_id, address)
VALUES (gen_random_uuid(), NOW(), 'master1', false, NULL, NULL, NOW(), 'master1', 'C 경기남부물류업체', 'SUPPLIER', '5fbaf44b-ddb7-4b35-8006-c04b6b730701', '경기도 이천시 덕평로 250');

INSERT INTO p_company (id, created_at, created_by, deleted, deleted_at, deleted_by, updated_at, updated_by, name, type, hub_id, address)
VALUES (gen_random_uuid(), NOW(), 'master1', false, NULL, NULL, NOW(), 'master1', 'D 부산물류업체', 'SUPPLIER', 'f7c49521-1203-44e5-9ea1-5bf66b0bc638', '부산 동구 중앙대로 200');

INSERT INTO p_company (id, created_at, created_by, deleted, deleted_at, deleted_by, updated_at, updated_by, name, type, hub_id, address)
VALUES (gen_random_uuid(), NOW(), 'master1', false, NULL, NULL, NOW(), 'master1', 'E 대구물류업체', 'SUPPLIER', '732b3153-c08d-4b00-9463-9fa94ed6fedb', '대구 북구 태평로 160');