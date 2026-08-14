import os
import sys
import json
import ctypes
import multiprocessing
import webview


def resource_path(relative_path):
    base_path = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
    return os.path.join(base_path, relative_path)


class ProjectApi:
    def __init__(self):
        self._window = None
        app_data = os.getenv('APPDATA') or os.path.expanduser('~')
        self._settings_dir = os.path.join(app_data, 'RebarQuantityCalculator')
        self._settings_path = os.path.join(self._settings_dir, 'settings.json')
        self.state_json = None
        self.current_path = None
        self.dirty = False

    def set_window(self, window):
        self._window = window

    def update_state(self, state_json, dirty=True):
        self.state_json = state_json
        if dirty:
            self.dirty = True
        return True

    def _suggested_filename(self, suggested_name):
        name = (suggested_name or '철근가공물량').strip()
        invalid = '<>:"/\\|?*'
        name = ''.join('_' if ch in invalid else ch for ch in name)
        if not name.endswith('.rebar.json'):
            name += '.rebar.json'
        return name

    def _write(self, path, state_json):
        parsed = json.loads(state_json)
        temp_path = path + '.tmp'
        with open(temp_path, 'w', encoding='utf-8') as file:
            json.dump(parsed, file, ensure_ascii=False, indent=2)
        os.replace(temp_path, path)
        self.current_path = path
        self.state_json = state_json
        self.dirty = False
        self._remember_path(path)

    def _remember_path(self, path):
        os.makedirs(self._settings_dir, exist_ok=True)
        temp_path = self._settings_path + '.tmp'
        with open(temp_path, 'w', encoding='utf-8') as file:
            json.dump({'last_project_path': path}, file, ensure_ascii=False)
        os.replace(temp_path, self._settings_path)

    def load_last_project(self):
        try:
            if not os.path.isfile(self._settings_path):
                return {'ok': False, 'not_found': True}
            with open(self._settings_path, 'r', encoding='utf-8') as file:
                settings = json.load(file)
            path = settings.get('last_project_path')
            if not path or not os.path.isfile(path):
                return {'ok': False, 'not_found': True}
            with open(path, 'r', encoding='utf-8') as file:
                parsed = json.load(file)
            self.current_path = path
            self.state_json = json.dumps(parsed, ensure_ascii=False)
            self.dirty = False
            return {'ok': True, 'path': path, 'name': os.path.basename(path), 'state': parsed}
        except Exception as error:
            return {'ok': False, 'error': str(error)}

    def _choose_save_path(self, suggested_name):
        selected = self._window.create_file_dialog(
            webview.SAVE_DIALOG,
            save_filename=self._suggested_filename(suggested_name),
            file_types=('철근 프로젝트 (*.rebar.json)', 'JSON 파일 (*.json)'),
        )
        if not selected:
            return None
        return selected[0] if isinstance(selected, (tuple, list)) else selected

    def save_project(self, state_json, suggested_name='철근가공물량', save_as=False):
        try:
            path = None if save_as else self.current_path
            if not path:
                path = self._choose_save_path(suggested_name)
            if not path:
                return {'ok': False, 'cancelled': True}
            self._write(path, state_json)
            return {'ok': True, 'path': path, 'name': os.path.basename(path)}
        except Exception as error:
            return {'ok': False, 'error': str(error)}

    def open_project(self):
        try:
            selected = self._window.create_file_dialog(
                webview.OPEN_DIALOG,
                allow_multiple=False,
                file_types=('철근 프로젝트 (*.rebar.json)', 'JSON 파일 (*.json)'),
            )
            if not selected:
                return {'ok': False, 'cancelled': True}
            path = selected[0] if isinstance(selected, (tuple, list)) else selected
            with open(path, 'r', encoding='utf-8') as file:
                parsed = json.load(file)
            state_json = json.dumps(parsed, ensure_ascii=False)
            self.current_path = path
            self.state_json = state_json
            self.dirty = False
            self._remember_path(path)
            return {'ok': True, 'path': path, 'name': os.path.basename(path), 'state': parsed}
        except Exception as error:
            return {'ok': False, 'error': str(error)}

    def on_closing(self):
        result = ctypes.windll.user32.MessageBoxW(
            None,
            '변경된 내용을 저장하시겠습니까?',
            '철근가공물량 산출 프로그램',
            0x00000003 | 0x00000020,
        )
        if result == 2:  # Cancel
            return False
        if result == 7:  # No
            return True
        if result == 6:  # Yes
            saved = self.save_project(self.state_json or '{}', '철근가공물량')
            return bool(saved.get('ok'))
        return False


def main():
    multiprocessing.freeze_support()
    api = ProjectApi()
    window = webview.create_window(
        "철근가공물량 산출 프로그램",
        resource_path("index.html"),
        width=1360,
        height=860,
        min_size=(1000, 650),
        js_api=api,
    )
    api.set_window(window)
    window.events.closing += api.on_closing
    webview.start()


if __name__ == "__main__":
    main()
