# Sử dụng hình ảnh JDK 17 làm base image
FROM eclipse-temurin:21-jdk-jammy

# Đặt biến môi trường
ENV APP_HOME=/usr/app/

# Tạo thư mục ứng dụng
RUN mkdir -p $APP_HOME

# Đặt thư mục hiện tại thành thư mục ứng dụng
WORKDIR $APP_HOME

# Sao chép file JAR đã đóng gói vào thư mục ứng dụng
COPY target/producer-0.0.1-SNAPSHOT.jar app.jar

# Mở cổng ứng dụng
EXPOSE 8080

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
