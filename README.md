# TaMa – Task Management Web Application

**TaMa** là ứng dụng web quản lý công việc theo mô hình **Board – List – Card**, được phát triển bằng Java, Spring Boot và MySQL.

Dự án được thực hiện theo nhóm trong khuôn khổ học phần tại HUTECH, tập trung vào việc xây dựng REST API, xử lý nghiệp vụ quản lý công việc, thiết kế cơ sở dữ liệu quan hệ và triển khai xác thực, phân quyền người dùng.

## 1. Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 17, Spring Boot, Spring MVC |
| Frontend | Thymeleaf, HTML, CSS, JavaScript, AJAX |
| Database | MySQL 8.4 |
| Data Access | Spring Data JPA, Hibernate |
| Security | Spring Security, Session, BCrypt, CSRF |
| Build Tool | Maven |
| Architecture | MVC, Controller – Service – Repository |

## 2. Các chức năng chính

### Quản lý tài khoản
- Đăng ký, đăng nhập và đăng xuất.
- Quản lý thông tin cá nhân và ảnh đại diện.
- Phân quyền hệ thống với hai vai trò `ADMIN` và `USER`.

### Quản lý Board
- Tạo, xem, chỉnh sửa và xóa Board.
- Tùy chỉnh màu sắc hoặc hình ảnh nền Board.
- Mỗi Board thuộc quyền sở hữu của một người dùng.

### Quản lý List
- Thêm, sửa và xóa List trong Board.
- Sắp xếp lại thứ tự List bằng thao tác kéo thả.
- Quản lý vị trí List trong từng Board.

### Quản lý Card
- Tạo, xem, chỉnh sửa và xóa Card.
- Kéo thả để thay đổi thứ tự hoặc chuyển Card giữa các List thuộc cùng Board.
- Thiết lập mức độ ưu tiên: `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
- Thiết lập ngày hết hạn (Due Date).
- Đánh dấu hoàn thành hoặc chưa hoàn thành công việc.
- Quản lý mô tả và các thông tin liên quan đến Card.

### Label và Comment
- Tạo, chỉnh sửa và xóa Label.
- Gán hoặc gỡ Label khỏi Card.
- Quản lý Label riêng theo từng người dùng.
- Thêm, chỉnh sửa và xóa Comment trên Card.

## 3. Thiết kế và kiến trúc hệ thống

Ứng dụng sử dụng Spring MVC kết hợp các REST API phục vụ thao tác dữ liệu từ giao diện.

**Luồng xử lý chính:**

`Thymeleaf / JavaScript (AJAX) → Controller → Service → Repository → MySQL`

Các thành phần đảm nhiệm:

- **Controller:** Tiếp nhận HTTP request, xử lý dữ liệu đầu vào và trả response.
- **Service:** Xử lý logic nghiệp vụ, kiểm tra quyền sở hữu tài nguyên và quản lý transaction.
- **Repository:** Truy xuất dữ liệu thông qua Spring Data JPA.
- **Entity:** Ánh xạ các đối tượng nghiệp vụ với bảng dữ liệu trong MySQL.
- **Frontend:** Hiển thị giao diện và gửi request đến REST API bằng JavaScript/AJAX.

Hệ thống phân tách xử lý giao diện, nghiệp vụ và truy xuất dữ liệu nhằm hỗ trợ việc bảo trì và mở rộng.

## 4. Database

Cơ sở dữ liệu `tama_db` gồm **9 bảng**:

| Bảng | Chức năng |
|---|---|
| `users` | Lưu thông tin tài khoản |
| `roles` | Định nghĩa vai trò hệ thống |
| `user_roles` | Liên kết người dùng và vai trò |
| `boards` | Quản lý Board |
| `board_lists` | Quản lý List thuộc Board |
| `cards` | Quản lý Card và trạng thái công việc |
| `labels` | Lưu Label theo người dùng |
| `card_labels` | Liên kết Card và Label |
| `comments` | Lưu bình luận trên Card |

Database sử dụng khóa chính, khóa ngoại, các ràng buộc dữ liệu và index phục vụ truy vấn.

Script khởi tạo:

`database/tama_db.sql`

**Lưu ý:** Script có các lệnh `DROP TABLE` để tái tạo cấu trúc database. Chỉ chạy trên database thử nghiệm hoặc khi chấp nhận xóa dữ liệu hiện có.

## 5. Authentication & Authorization

Hệ thống sử dụng **Spring Security** để quản lý xác thực và kiểm soát quyền truy cập.

- Xác thực người dùng bằng Session-based Authentication.
- Băm mật khẩu bằng BCrypt trước khi lưu vào database.
- Sử dụng CSRF protection cho các thao tác thay đổi dữ liệu.
- Phân quyền truy cập đối với các chức năng quản trị.
- Kiểm tra quyền sở hữu tài nguyên tại tầng Service để hạn chế truy cập hoặc chỉnh sửa dữ liệu của người dùng khác.

Ví dụ: Người dùng chỉ được thao tác với Board và các tài nguyên thuộc Board mà mình sở hữu.

## 6. Cài đặt và chạy dự án

### Yêu cầu môi trường

- JDK 17
- MySQL 8.x
- Git
- IDE hỗ trợ Java/Spring Boot (khuyến nghị IntelliJ IDEA)

Dự án đã tích hợp Maven Wrapper.

### Bước 1 – Clone repository

```bash
git clone https://github.com/longchau5823/tama_task_management.git
cd tama_task_management
```

### Bước 2 – Khởi tạo database

Mở MySQL Workbench, HeidiSQL hoặc công cụ quản lý MySQL tương đương.

Thực thi file:

`database/tama_db.sql`

Script sẽ tạo database `tama_db`, các bảng và dữ liệu vai trò `ADMIN`, `USER` ban đầu.

### Bước 3 – Cấu hình kết nối MySQL

Mở file:

`src/main/resources/application.properties`

Điều chỉnh thông tin kết nối phù hợp với môi trường local:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tama_db?serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
```

Không đưa mật khẩu hoặc thông tin kết nối nhạy cảm lên repository công khai.

Ứng dụng sử dụng `spring.jpa.hibernate.ddl-auto=validate`, vì vậy cần khởi tạo schema trước khi chạy.

### Bước 4 – Khởi chạy ứng dụng

Trên Windows (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

Trên Linux/macOS:

```bash
./mvnw spring-boot:run
```

Hoặc chạy class `TamaApplication.java` bằng IDE.

Sau khi ứng dụng khởi động, truy cập:

**http://localhost:8080**

Đăng ký tài khoản mới để bắt đầu sử dụng.

## 7. Vai trò và đóng góp cá nhân

**Châu Thuyên Long – Team Leader & Main Developer**

Trong quá trình thực hiện dự án, tôi đảm nhiệm các công việc chính:

- Tham gia phân tích yêu cầu và xác định phạm vi chức năng.
- Thiết kế cấu trúc cơ sở dữ liệu và các đối tượng nghiệp vụ.
- Phát triển các chức năng backend bằng Spring Boot và REST API.
- Triển khai các nghiệp vụ Board, List, Card, Label, Comment và xử lý kéo thả.
- Xây dựng cơ chế xác thực, phân quyền và kiểm tra quyền sở hữu dữ liệu.
- Tích hợp backend với giao diện Thymeleaf/JavaScript.
- Phối hợp với các thành viên trong quá trình phát triển và hoàn thiện dự án.

## 8. Thông tin dự án

- **Tên dự án:** TaMa – Task Management Web Application
- **Loại dự án:** Đồ án học phần, phát triển theo nhóm
- **Trường:** Ho Chi Minh City University of Technology (HUTECH)
- **Repository:** https://github.com/longchau5823/tama_task_management

**Developer:** [Châu Thuyên Long](https://github.com/longchau5823)
