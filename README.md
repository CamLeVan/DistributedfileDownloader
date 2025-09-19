1. Tổng Quan Dự Án
Đây là một ứng dụng phân tán (distributed application) cho phép tải xuống một tệp tin lớn bằng cách chia nhỏ nó thành các phần và tải các phần đó đồng thời từ nhiều máy chủ khác nhau. Sau khi tải xong, ứng dụng sẽ ghép các phần lại và tiến hành xác thực tính toàn vẹn của tệp tin để đảm bảo dữ liệu không bị lỗi hoặc can thiệp trong quá trình truyền tải.
Mục Tiêu Chính:
Tăng tốc độ tải xuống: Tận dụng băng thông từ nhiều nguồn (máy chủ) để tải các phần của file song song.
Đảm bảo tính tin cậy: Xác minh xem file tải về có giống hoàn toàn với file gốc hay không thông qua thuật toán băm.
Bài tập thực hành: Áp dụng các kiến thức về lập trình mạng, đa luồng và giao thức TCP vào một bài toán thực tế.
Phương Châm: "Chia để trị, tải cho nhanh, kiểm tra cho chắc."
