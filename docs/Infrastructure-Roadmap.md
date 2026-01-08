# UNBOX 인프라 고도화 로드맵

## 📋 개요
UNBOX 서비스의 성장 단계별 인프라 고도화 계획을 정의합니다.

---

## 🎯 1차 고도화 (3-6개월) - 안정성 및 모니터링 강화

### 목표
- **가용성**: 99.5% → 99.9%
- **모니터링**: 실시간 알람 및 대시보드 구축
- **보안**: 기본 보안 강화
- **성능**: 응답시간 최적화

### 인프라 개선사항

#### 1.1 모니터링 및 알람 시스템 구축

**CloudWatch 통합 모니터링**
```yaml
# 핵심 메트릭 설정
메트릭:
  - CPU 사용률: 80% 이상 시 알람
  - 메모리 사용률: 85% 이상 시 알람
  - 스왑 사용률: 20% 이상 시 알람
  - 디스크 사용률: 80% 이상 시 알람
  - API 응답시간: 2초 이상 시 알람
  - 에러율: 5% 이상 시 알람

알람 채널:
  - Slack 통합
  - 이메일 알림
  - SMS (중요 알람)
```

**로그 중앙화**
```bash
# ELK Stack 도입
Elasticsearch: 로그 저장 및 검색
Logstash: 로그 수집 및 파싱
Kibana: 로그 시각화 및 대시보드

# 로그 수집 대상
- Spring Boot 애플리케이션 로그
- Nginx 액세스 로그
- 시스템 로그 (syslog)
- Docker 컨테이너 로그
```

#### 1.2 고가용성 구성

**Multi-AZ RDS 구성**
```terraform
# RDS 고가용성 설정
resource "aws_db_instance" "postgres" {
  multi_az               = true    # Multi-AZ 활성화
  backup_retention_period = 7     # 7일 백업 보관
  backup_window          = "03:00-04:00"  # 새벽 백업
  maintenance_window     = "sun:04:00-sun:05:00"  # 일요일 새벽 유지보수
  
  # 자동 백업 및 스냅샷
  copy_tags_to_snapshot = true
  delete_automated_backups = false
}
```

**Application Load Balancer 도입**
```terraform
# ALB 구성
resource "aws_lb" "main" {
  name               = "unbox-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets           = [aws_subnet.public_a.id, aws_subnet.public_c.id]

  # 헬스체크 설정
  health_check {
    enabled             = true
    healthy_threshold   = 2
    interval            = 30
    matcher             = "200"
    path                = "/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    timeout             = 5
    unhealthy_threshold = 2
  }
}
```

#### 1.3 보안 강화

**WAF (Web Application Firewall) 도입**
```terraform
# AWS WAF 설정
resource "aws_wafv2_web_acl" "main" {
  name  = "unbox-waf"
  scope = "REGIONAL"

  # 기본 보안 규칙
  rule {
    name     = "AWSManagedRulesCommonRuleSet"
    priority = 1
    
    override_action {
      none {}
    }
    
    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesCommonRuleSet"
        vendor_name = "AWS"
      }
    }
  }
  
  # SQL 인젝션 방지
  rule {
    name     = "AWSManagedRulesSQLiRuleSet"
    priority = 2
    
    override_action {
      none {}
    }
    
    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesSQLiRuleSet"
        vendor_name = "AWS"
      }
    }
  }
}
```

**SSL/TLS 인증서 적용**
```terraform
# ACM 인증서
resource "aws_acm_certificate" "main" {
  domain_name       = "api.unbox.com"
  validation_method = "DNS"
  
  lifecycle {
    create_before_destroy = true
  }
}

# HTTPS 리스너
resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.main.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = aws_acm_certificate.main.arn
}
```

#### 1.4 성능 최적화

**Redis 클러스터 구성**
```terraform
# ElastiCache Redis 클러스터
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "unbox-redis"
  description                = "Redis cluster for UNBOX"
  
  node_type                  = "cache.t3.micro"
  port                       = 6379
  parameter_group_name       = "default.redis7"
  
  num_cache_clusters         = 2
  automatic_failover_enabled = true
  multi_az_enabled          = true
  
  subnet_group_name = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]
}
```

**CDN (CloudFront) 도입**
```terraform
# CloudFront 배포
resource "aws_cloudfront_distribution" "main" {
  origin {
    domain_name = aws_lb.main.dns_name
    origin_id   = "ALB-unbox"
    
    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }
  
  # 캐싱 설정
  default_cache_behavior {
    allowed_methods        = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "ALB-unbox"
    compress               = true
    viewer_protocol_policy = "redirect-to-https"
    
    # API는 캐싱하지 않음
    cache_policy_id = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"  # CachingDisabled
  }
  
  # 정적 리소스 캐싱
  ordered_cache_behavior {
    path_pattern     = "/static/*"
    allowed_methods  = ["GET", "HEAD"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "ALB-unbox"
    compress         = true
    
    cache_policy_id = "658327ea-f89d-4fab-a63d-7e88639e58f6"  # CachingOptimized
  }
}
```

### 서비스 개선사항

#### 1.5 애플리케이션 최적화

**데이터베이스 최적화**
```sql
-- 인덱스 최적화
CREATE INDEX CONCURRENTLY idx_products_category_created 
ON products(category_id, created_at DESC);

CREATE INDEX CONCURRENTLY idx_orders_user_status 
ON orders(user_id, status, created_at DESC);

-- 파티셔닝 (주문 테이블)
CREATE TABLE orders_2026_01 PARTITION OF orders 
FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
```

**캐싱 전략 구현**
```java
// 상품 목록 캐싱
@Cacheable(value = "products", key = "#categoryId + '_' + #page")
public Page<Product> getProductsByCategory(Long categoryId, Pageable page) {
    return productRepository.findByCategoryId(categoryId, page);
}

// 사용자 세션 캐싱
@Cacheable(value = "user_sessions", key = "#userId")
public UserSession getUserSession(Long userId) {
    return userSessionRepository.findByUserId(userId);
}
```

**API 최적화**
```java
// N+1 문제 해결
@Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id IN :ids")
List<Product> findProductsWithCategory(@Param("ids") List<Long> ids);

// 페이징 최적화
@Query(value = "SELECT * FROM products WHERE id > :lastId ORDER BY id LIMIT :size", 
       nativeQuery = true)
List<Product> findProductsAfter(@Param("lastId") Long lastId, @Param("size") int size);
```

---

## 🚀 2차 고도화 (6-12개월) - 확장성 및 마이크로서비스

### 목표
- **확장성**: Auto Scaling 및 수평 확장
- **아키텍처**: 마이크로서비스 전환 준비
- **데이터**: 빅데이터 파이프라인 구축
- **글로벌**: 다중 리전 지원 준비

### 인프라 개선사항

#### 2.1 컨테이너 오케스트레이션 (EKS)

**Amazon EKS 클러스터 구성**
```yaml
# EKS 클러스터 설정
apiVersion: v1
kind: ConfigMap
metadata:
  name: unbox-config
data:
  # 환경별 설정
  spring.profiles.active: "production"
  spring.datasource.url: "jdbc:postgresql://unbox-rds.cluster-xxx.ap-northeast-2.rds.amazonaws.com:5432/unboxdb"
  
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: unbox-backend
spec:
  replicas: 3  # 3개 인스턴스로 확장
  selector:
    matchLabels:
      app: unbox-backend
  template:
    metadata:
      labels:
        app: unbox-backend
    spec:
      containers:
      - name: unbox-backend
        image: gahyunsong/unbox-backend-v2:latest
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        # 헬스체크
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

**Auto Scaling 설정**
```yaml
# Horizontal Pod Autoscaler
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: unbox-backend-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: unbox-backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

#### 2.2 마이크로서비스 아키텍처

**서비스 분리 계획**
```
기존 모놀리식 → 마이크로서비스 분리

1. User Service (사용자 관리)
   - 회원가입/로그인
   - 프로필 관리
   - 권한 관리

2. Product Service (상품 관리)
   - 상품 CRUD
   - 카테고리 관리
   - 재고 관리

3. Order Service (주문 관리)
   - 주문 생성/조회
   - 결제 처리
   - 주문 상태 관리

4. Notification Service (알림)
   - 이메일 발송
   - SMS 발송
   - 푸시 알림
```

**API Gateway 구성**
```yaml
# Kong API Gateway 설정
apiVersion: configuration.konghq.com/v1
kind: KongIngress
metadata:
  name: unbox-api-gateway
proxy:
  connect_timeout: 10000
  retries: 3
  read_timeout: 10000
  write_timeout: 10000
route:
  strip_path: true
  preserve_host: true

---
# 라우팅 규칙
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: unbox-ingress
  annotations:
    kubernetes.io/ingress.class: kong
    konghq.com/plugins: rate-limiting, cors
spec:
  rules:
  - host: api.unbox.com
    http:
      paths:
      - path: /api/users
        pathType: Prefix
        backend:
          service:
            name: user-service
            port:
              number: 8080
      - path: /api/products
        pathType: Prefix
        backend:
          service:
            name: product-service
            port:
              number: 8080
      - path: /api/orders
        pathType: Prefix
        backend:
          service:
            name: order-service
            port:
              number: 8080
```

#### 2.3 데이터 파이프라인 구축

**실시간 데이터 스트리밍**
```yaml
# Apache Kafka 클러스터
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: unbox-kafka
spec:
  kafka:
    version: 3.5.0
    replicas: 3
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
      - name: tls
        port: 9093
        type: internal
        tls: true
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
    storage:
      type: jbod
      volumes:
      - id: 0
        type: persistent-claim
        size: 100Gi
        deleteClaim: false
  zookeeper:
    replicas: 3
    storage:
      type: persistent-claim
      size: 10Gi
      deleteClaim: false
```

**데이터 웨어하우스 구성**
```sql
-- Amazon Redshift 스키마 설계
CREATE SCHEMA analytics;

-- 사용자 행동 분석 테이블
CREATE TABLE analytics.user_events (
    event_id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT,
    event_type VARCHAR(50),
    event_data JSON,
    created_at TIMESTAMP,
    session_id VARCHAR(36)
) DISTKEY(user_id) SORTKEY(created_at);

-- 상품 성과 분석 테이블
CREATE TABLE analytics.product_metrics (
    product_id BIGINT,
    date_key DATE,
    views INTEGER,
    purchases INTEGER,
    revenue DECIMAL(10,2),
    conversion_rate DECIMAL(5,4)
) DISTKEY(product_id) SORTKEY(date_key);
```

#### 2.4 글로벌 확장 준비

**다중 리전 구성**
```terraform
# 글로벌 인프라 설정
# Primary Region: ap-northeast-2 (Seoul)
# Secondary Region: us-west-2 (Oregon)

# Route 53 헬스체크 및 페일오버
resource "aws_route53_health_check" "primary" {
  fqdn                            = "api-seoul.unbox.com"
  port                            = 443
  type                            = "HTTPS"
  resource_path                   = "/actuator/health"
  failure_threshold               = "3"
  request_interval                = "30"
  cloudwatch_alarm_region         = "ap-northeast-2"
  cloudwatch_alarm_name           = "unbox-primary-health"
  insufficient_data_health_status = "Failure"
}

# DNS 페일오버 설정
resource "aws_route53_record" "primary" {
  zone_id = aws_route53_zone.main.zone_id
  name    = "api.unbox.com"
  type    = "A"
  
  set_identifier = "primary"
  failover_routing_policy {
    type = "PRIMARY"
  }
  
  health_check_id = aws_route53_health_check.primary.id
  
  alias {
    name                   = aws_cloudfront_distribution.seoul.domain_name
    zone_id                = aws_cloudfront_distribution.seoul.hosted_zone_id
    evaluate_target_health = true
  }
}
```

### 서비스 개선사항

#### 2.5 고급 기능 구현

**실시간 추천 시스템**
```python
# Apache Spark를 이용한 실시간 추천
from pyspark.sql import SparkSession
from pyspark.ml.recommendation import ALS
from pyspark.ml.evaluation import RegressionEvaluator

# 협업 필터링 모델
def train_recommendation_model(spark, ratings_df):
    als = ALS(
        maxIter=10,
        regParam=0.1,
        userCol="user_id",
        itemCol="product_id",
        ratingCol="rating",
        coldStartStrategy="drop"
    )
    
    model = als.fit(ratings_df)
    return model

# 실시간 추천 API
@app.route('/api/recommendations/<int:user_id>')
def get_recommendations(user_id):
    recommendations = model.recommendForUsers(
        spark.createDataFrame([(user_id,)], ["user_id"]),
        numItems=10
    )
    return jsonify(recommendations.collect())
```

**이벤트 소싱 패턴**
```java
// 주문 이벤트 소싱
@Entity
public class OrderEvent {
    @Id
    private String eventId;
    private Long orderId;
    private String eventType;
    private String eventData;
    private LocalDateTime createdAt;
    
    // 이벤트 타입
    public enum EventType {
        ORDER_CREATED,
        ORDER_PAID,
        ORDER_SHIPPED,
        ORDER_DELIVERED,
        ORDER_CANCELLED
    }
}

// 이벤트 스토어
@Service
public class OrderEventStore {
    
    @Autowired
    private OrderEventRepository eventRepository;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public void saveEvent(OrderEvent event) {
        // 이벤트 저장
        eventRepository.save(event);
        
        // Kafka로 이벤트 발행
        kafkaTemplate.send("order-events", event);
    }
    
    public List<OrderEvent> getOrderHistory(Long orderId) {
        return eventRepository.findByOrderIdOrderByCreatedAt(orderId);
    }
}
```

**CQRS (Command Query Responsibility Segregation)**
```java
// Command 모델 (쓰기)
@Service
public class OrderCommandService {
    
    public void createOrder(CreateOrderCommand command) {
        Order order = new Order(command);
        orderRepository.save(order);
        
        // 이벤트 발행
        eventPublisher.publishEvent(new OrderCreatedEvent(order));
    }
}

// Query 모델 (읽기)
@Service
public class OrderQueryService {
    
    @Autowired
    private OrderReadModelRepository readModelRepository;
    
    public OrderSummary getOrderSummary(Long orderId) {
        return readModelRepository.findOrderSummary(orderId);
    }
    
    public Page<OrderListItem> getOrderList(Long userId, Pageable pageable) {
        return readModelRepository.findOrdersByUserId(userId, pageable);
    }
}
```

---

## 📊 성과 지표 및 목표

### 1차 고도화 목표
| 지표 | 현재 | 목표 | 측정 방법 |
|------|------|------|-----------|
| 가용성 | 99.5% | 99.9% | CloudWatch 업타임 모니터링 |
| 응답시간 | 0.8초 | 0.5초 | API 응답시간 측정 |
| 동시접속 | 200명 | 500명 | 로드 테스트 |
| 장애 복구 시간 | 5분 | 2분 | 평균 복구 시간 측정 |
| 모니터링 커버리지 | 60% | 95% | 메트릭 수집 비율 |

### 2차 고도화 목표
| 지표 | 1차 후 | 목표 | 측정 방법 |
|------|--------|------|-----------|
| 확장성 | 수동 | 자동 | Auto Scaling 동작 확인 |
| 배포 시간 | 5분 | 30초 | 무중단 배포 시간 |
| 서비스 분리도 | 모놀리식 | 4개 서비스 | 마이크로서비스 개수 |
| 데이터 처리량 | 배치 | 실시간 | 스트리밍 처리 지연시간 |
| 글로벌 지연시간 | N/A | <200ms | 리전별 응답시간 |

---

## 💰 비용 예상

### 1차 고도화 비용 (월 기준)
```
현재 비용: $8.5
추가 비용:
- CloudWatch: $10
- ALB: $16
- ACM: $0 (무료)
- WAF: $5
- ElastiCache: $13
총 비용: $52.5/월 (6배 증가, 하지만 안정성 크게 향상)
```

### 2차 고도화 비용 (월 기준)
```
1차 후 비용: $52.5
추가 비용:
- EKS: $73 (클러스터 $73 + 노드 그룹)
- Kafka: $45
- Redshift: $160
- 추가 리전: $100
총 비용: $430.5/월 (엔터프라이즈급 인프라)
```

---

## 🎯 마이그레이션 전략

### 1차 고도화 마이그레이션
1. **준비 단계** 
   - 모니터링 도구 설치
   - 백업 및 롤백 계획 수립
   
2. **점진적 적용** 
   - Multi-AZ RDS 전환
   - ALB 도입 및 트래픽 분산
   - SSL 인증서 적용
   
3. **검증 및 최적화** 
   - 성능 테스트
   - 모니터링 대시보드 구축

### 2차 고도화 마이그레이션
1. **서비스 분석**
   - 도메인 경계 식별
   - 데이터 의존성 분석
   
2. **Strangler Fig 패턴** 
   - 새로운 마이크로서비스 개발
   - 점진적 트래픽 이전
   - 레거시 코드 제거
   
3. **데이터 파이프라인** 
   - Kafka 클러스터 구축
   - 실시간 스트리밍 구현
   - 분석 대시보드 구축
