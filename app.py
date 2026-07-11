import os
import sys
import webview


def resource_path(relative_path):
    base_path = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
    return os.path.join(base_path, relative_path)


def main():
    webview.create_window(
        "철근가공물량 산출 프로그램",
        resource_path("index.html"),
        width=1360,
        height=860,
        min_size=(1000, 650),
    )
    webview.start()


if __name__ == "__main__":
    main()
