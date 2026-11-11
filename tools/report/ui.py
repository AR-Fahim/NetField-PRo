#!/usr/bin/env python3
import argparse
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

def get_serial():
    return os.environ.get("ANDROID_SERIAL")

def adb_cmd(args, capture_output=True, check=True):
    serial = get_serial()
    cmd = ["adb"]
    if serial:
        cmd.extend(["-s", serial])
    cmd.extend(args)
    res = subprocess.run(cmd, capture_output=capture_output, text=True)
    if check and res.returncode != 0:
        raise RuntimeError(f"Command failed (code {res.returncode}): {' '.join(cmd)}\nStderr: {res.stderr}")
    return res

def adb_shell(cmd_str, check=True):
    return adb_cmd(["shell", cmd_str], check=check)

def pull_xml_dump(remote_path="/sdcard/ui.xml", local_path="ui_dump.xml"):
    adb_shell(f"uiautomator dump {remote_path}")
    adb_cmd(["pull", remote_path, local_path])
    adb_shell(f"rm {remote_path}")
    tree = ET.parse(local_path)
    if os.path.exists(local_path):
        os.remove(local_path)
    return tree

def parse_bounds(bounds_str):
    # bounds="[0,0][1080,2400]"
    m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds_str)
    if not m:
        return None
    return int(m.group(1)), int(m.group(2)), int(m.group(3)), int(m.group(4))

def find_node(root, id_suffix=None, text=None, desc=None):
    for node in root.iter("node"):
        res_id = node.attrib.get("resource-id", "")
        node_text = node.attrib.get("text", "")
        node_desc = node.attrib.get("content-desc", "")

        if id_suffix:
            if res_id.endswith(f":id/{id_suffix}") or res_id == id_suffix:
                return node
        if text:
            if node_text == text or text in node_text:
                return node
        if desc:
            if node_desc == desc or desc in node_desc:
                return node
    return None

def do_shot(name):
    os.makedirs("docs/report/img", exist_ok=True)
    out_path = os.path.join("docs/report/img", f"{name}.png")
    remote_path = "/sdcard/_s.png"
    adb_shell(f"screencap -p {remote_path}")
    adb_cmd(["pull", remote_path, out_path])
    adb_shell(f"rm {remote_path}")
    size = os.path.getsize(out_path)
    if size < 10240:
        raise RuntimeError(f"Screenshot {out_path} is under 10 KB ({size} bytes)")
    print(f"Shot saved to {out_path} ({size} bytes)")

def do_dump():
    tree = pull_xml_dump()
    print("UI hierarchy dump:")
    for node in tree.iter("node"):
        res_id = node.attrib.get("resource-id", "")
        text = node.attrib.get("text", "")
        desc = node.attrib.get("content-desc", "")
        bounds = node.attrib.get("bounds", "")
        cls = node.attrib.get("class", "")
        if text or desc or res_id:
            print(f"  {cls} id='{res_id}' text='{text}' desc='{desc}' bounds='{bounds}'")

def do_wait(id_suffix=None, text=None, desc=None, timeout=15):
    start = time.time()
    while time.time() - start < timeout:
        try:
            tree = pull_xml_dump()
            node = find_node(tree.getroot(), id_suffix=id_suffix, text=text, desc=desc)
            if node is not None:
                print(f"Found node matching id='{id_suffix}' text='{text}' desc='{desc}'")
                return True
        except Exception:
            pass
        time.sleep(1)
    raise TimeoutError(f"Timed out after {timeout}s waiting for id='{id_suffix}' text='{text}' desc='{desc}'")

def do_tap(id_suffix=None, text=None, desc=None, xy=None, frac=None):
    if xy:
        x, y = xy
        adb_shell(f"input tap {x} {y}")
        print(f"Tapped coordinates ({x}, {y})")
        return

    for attempt in range(5):
        try:
            tree = pull_xml_dump()
            target_id = id_suffix if id_suffix else (frac[0] if frac else None)
            node = find_node(tree.getroot(), id_suffix=target_id, text=text, desc=desc)
            if node is not None:
                bounds = parse_bounds(node.attrib.get("bounds", ""))
                if bounds:
                    l, t, r, b = bounds
                    if frac:
                        fx, fy = float(frac[1]), float(frac[2])
                        x = int(l + (r - l) * fx)
                        y = int(t + (b - t) * fy)
                    else:
                        x = (l + r) // 2
                        y = (t + b) // 2
                    adb_shell(f"input tap {x} {y}")
                    print(f"Tapped node (attempt {attempt + 1}) at ({x}, {y}) [bounds {l},{t},{r},{b}]")
                    return
        except Exception as e:
            pass
        time.sleep(1)
    raise RuntimeError(f"Failed to find and tap target id='{id_suffix}' text='{text}' desc='{desc}' after 5 attempts")

def do_type(text, secret=False):
    escaped = text.replace(" ", "%s")
    adb_shell(f"input text {escaped}")
    if secret:
        print("Typed secret text [REDACTED]")
    else:
        print(f"Typed text: {text}")

def do_typeenv(var_name):
    val = os.environ.get(var_name)
    if not val:
        raise ValueError(f"Environment variable {var_name} is empty or unset")
    escaped = val.replace(" ", "%s")
    adb_shell(f"input text {escaped}")
    print(f"Typed value from environment variable {var_name} [REDACTED]")

def do_key(key_name):
    key_map = {
        "BACK": "4",
        "HOME": "3",
        "ENTER": "66"
    }
    code = key_map.get(key_name.upper())
    if not code:
        raise ValueError(f"Unknown key: {key_name}")
    adb_shell(f"input keyevent {code}")
    print(f"Pressed key {key_name} (code {code})")

def do_swipe(x1, y1, x2, y2, ms):
    adb_shell(f"input swipe {x1} {y1} {x2} {y2} {ms}")
    print(f"Swiped ({x1},{y1}) -> ({x2},{y2}) in {ms} ms")

def do_hold(x, y, ms):
    adb_shell(f"input swipe {x} {y} {x} {y} {ms}")
    print(f"Held at ({x},{y}) for {ms} ms")

def do_shade():
    adb_shell("cmd statusbar expand-notifications")
    print("Expanded notification shade")

def do_unshade():
    adb_shell("cmd statusbar collapse")
    print("Collapsed notification shade")

def main():
    parser = argparse.ArgumentParser(description="NetField Pro UI Driver")
    subparsers = parser.add_subparsers(dest="subcommand", required=True)

    # shot
    p_shot = subparsers.add_parser("shot")
    p_shot.add_argument("name", help="Screenshot base name")

    # dump
    subparsers.add_parser("dump")

    # tap
    p_tap = subparsers.add_parser("tap")
    p_tap.add_argument("--id", dest="id_suffix")
    p_tap.add_argument("--text")
    p_tap.add_argument("--desc")
    p_tap.add_argument("--xy", nargs=2, type=int)
    p_tap.add_argument("--frac", nargs=3)

    # wait
    p_wait = subparsers.add_parser("wait")
    p_wait.add_argument("--id", dest="id_suffix")
    p_wait.add_argument("--text")
    p_wait.add_argument("--desc")
    p_wait.add_argument("--timeout", type=int, default=15)

    # type
    p_type = subparsers.add_parser("type")
    p_type.add_argument("text")
    p_type.add_argument("--secret", action="store_true")

    # typeenv
    p_typeenv = subparsers.add_parser("typeenv")
    p_typeenv.add_argument("var_name")

    # key
    p_key = subparsers.add_parser("key")
    p_key.add_argument("key_name", choices=["BACK", "HOME", "ENTER", "back", "home", "enter"])

    # swipe
    p_swipe = subparsers.add_parser("swipe")
    p_swipe.add_argument("x1", type=int)
    p_swipe.add_argument("y1", type=int)
    p_swipe.add_argument("x2", type=int)
    p_swipe.add_argument("y2", type=int)
    p_swipe.add_argument("ms", type=int)

    # hold
    p_hold = subparsers.add_parser("hold")
    p_hold.add_argument("x", type=int)
    p_hold.add_argument("y", type=int)
    p_hold.add_argument("ms", type=int)

    # shade / unshade
    subparsers.add_parser("shade")
    subparsers.add_parser("unshade")

    args = parser.parse_args()

    if args.subcommand == "shot":
        do_shot(args.name)
    elif args.subcommand == "dump":
        do_dump()
    elif args.subcommand == "tap":
        do_tap(id_suffix=args.id_suffix, text=args.text, desc=args.desc, xy=args.xy, frac=args.frac)
    elif args.subcommand == "wait":
        do_wait(id_suffix=args.id_suffix, text=args.text, desc=args.desc, timeout=args.timeout)
    elif args.subcommand == "type":
        do_type(args.text, secret=args.secret)
    elif args.subcommand == "typeenv":
        do_typeenv(args.var_name)
    elif args.subcommand == "key":
        do_key(args.key_name)
    elif args.subcommand == "swipe":
        do_swipe(args.x1, args.y1, args.x2, args.y2, args.ms)
    elif args.subcommand == "hold":
        do_hold(args.x, args.y, args.ms)
    elif args.subcommand == "shade":
        do_shade()
    elif args.subcommand == "unshade":
        do_unshade()

if __name__ == "__main__":
    main()

