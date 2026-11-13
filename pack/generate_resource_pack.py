#!/usr/bin/env python3
"""Generate deterministic 26.3 EvolutionSMP orb textures and a resource-pack ZIP."""
import json, math, os, struct, zlib, zipfile, argparse, shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent

def png_rgba(path, pixels, width=32, height=32):
    raw = b''.join(b'\x00' + bytes(sum((list(px) for px in pixels[y*width:(y+1)*width]), [])) for y in range(height))
    def chunk(kind, data): return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    path.parent.mkdir(parents=True, exist_ok=True); path.write_bytes(data)

def blank_canvas():
    return [(0,0,0,0)]*1024

def put_pixel(p,x,y,color,alpha=255):
    if 0<=x<32 and 0<=y<32: p[y*32+x]=(*color,alpha)

def draw_line(p,x0,y0,x1,y1,color,width=1):
    steps=max(abs(x1-x0),abs(y1-y0),1)*2
    for n in range(steps+1):
        t=n/steps; x=round(x0+(x1-x0)*t); y=round(y0+(y1-y0)*t); r=max(0,width//2)
        for ox in range(-r,r+1):
            for oy in range(-r,r+1): put_pixel(p,x+ox,y+oy,color)

def draw_polygon(p,points,fill,outline=None,width=1):
    for y in range(max(0,min(v[1] for v in points)),min(31,max(v[1] for v in points))+1):
        for x in range(max(0,min(v[0] for v in points)),min(31,max(v[0] for v in points))+1):
            inside=False; j=len(points)-1
            for i in range(len(points)):
                xi,yi=points[i]; xj,yj=points[j]
                if ((yi>y)!=(yj>y)) and x<(xj-xi)*(y-yi)/((yj-yi) or 1)+xi: inside=not inside
                j=i
            if inside: put_pixel(p,x,y,fill)
    if outline:
        for i,(x0,y0) in enumerate(points):
            x1,y1=points[(i+1)%len(points)]; draw_line(p,x0,y0,x1,y1,outline,width)

def draw_circle(p,cx,cy,r,color,outline=False):
    for y in range(max(0,cy-r),min(31,cy+r)+1):
        for x in range(max(0,cx-r),min(31,cx+r)+1):
            d=(x-cx)**2+(y-cy)**2
            if d<=r*r and (not outline or d>=(r-2)**2): put_pixel(p,x,y,color)

def orb_texture(model_id):
    """Original pixel art with distinct Tier II/III/IV shapes and separate Echo Shard geometry."""
    p=blank_canvas()
    if model_id==7801:
        # Tier II: tall hexagonal amethyst prism with six long planar faces.
        draw_polygon(p,[(16,1),(25,7),(28,17),(16,31),(4,17),(7,7)],(25,11,53),(9,4,25),2)
        draw_polygon(p,[(16,3),(23,8),(25,17),(16,28),(7,17),(9,8)],(81,39,145),(194,139,255),1)
        draw_polygon(p,[(16,3),(16,16),(9,8)],(231,201,255))
        draw_polygon(p,[(16,3),(23,8),(16,16)],(151,92,216))
        draw_polygon(p,[(9,8),(16,16),(7,17)],(184,126,240))
        draw_polygon(p,[(23,8),(25,17),(16,16)],(102,53,172))
        draw_polygon(p,[(7,17),(16,16),(16,28)],(130,72,190))
        draw_polygon(p,[(16,16),(25,17),(16,28)],(61,27,114))
        draw_line(p,16,3,16,28,(247,227,255)); draw_line(p,9,8,16,16,(255,245,255))
        put_pixel(p,24,6,(255,245,255))
    elif model_id==7802:
        # Tier III: eight-pointed astral star; rays are part of its outline, not a recolor.
        outer=[(16,0),(19,9),(27,4),(23,12),(31,16),(23,19),(28,28),(19,23),(16,31),(13,23),(4,28),(9,19),(0,16),(9,13),(4,4),(13,9)]
        draw_polygon(p,outer,(7,30,65),(2,12,31),2)
        inner=[(16,3),(19,11),(26,7),(22,14),(28,16),(22,18),(25,25),(18,21),(16,28),(14,21),(7,25),(10,18),(3,16),(10,14),(7,7),(14,11)]
        draw_polygon(p,inner,(21,112,171),(67,211,255))
        draw_polygon(p,[(16,5),(20,14),(16,16),(12,14)],(204,249,255))
        draw_polygon(p,[(16,16),(22,14),(21,20),(16,26),(11,20),(10,14)],(31,175,219),(171,244,255))
        draw_polygon(p,[(16,16),(20,14),(22,14),(16,19)],(255,255,238))
        draw_line(p,16,2,16,9,(255,255,255)); put_pixel(p,25,8,(255,255,255))
    elif model_id==7803:
        # Tier IV: jagged crowned crystal with a fractured obsidian heart.
        outer=[(8,5),(13,9),(16,1),(19,9),(25,4),(24,13),(31,16),(24,21),(26,27),(18,25),(13,31),(9,24),(2,23),(7,17),(2,11),(10,12)]
        draw_polygon(p,outer,(34,5,18),(16,2,10),2)
        inner=[(8,7),(13,11),(16,4),(19,11),(24,7),(22,15),(28,16),(22,21),(23,25),(17,23),(13,28),(10,22),(5,22),(9,17),(5,12),(12,13)]
        draw_polygon(p,inner,(132,10,38),(255,61,91))
        draw_polygon(p,[(13,11),(16,14),(13,19),(9,16)],(255,138,153))
        draw_polygon(p,[(16,14),(22,15),(21,21),(16,25),(13,19)],(48,3,21),(255,118,142))
        draw_polygon(p,[(16,14),(19,11),(22,15),(18,17)],(235,39,75))
        draw_line(p,16,14,14,19,(255,204,212)); draw_line(p,14,19,18,21,(255,101,128)); draw_line(p,18,21,16,25,(255,204,212))
    elif model_id==7804:
        # Echo Shard: slim, leaning splinter silhouette.
        draw_polygon(p,[(12,0),(20,5),(19,12),(25,17),(20,30),(14,25),(9,21),(5,15),(9,9)],(27,14,49),(11,5,25),2)
        draw_polygon(p,[(12,2),(18,6),(17,13),(22,18),(19,27),(15,23),(10,20),(7,15),(11,9)],(76,45,132),(175,131,255))
        draw_polygon(p,[(12,2),(13,13),(10,20),(7,15),(11,9)],(208,177,255))
        draw_polygon(p,[(13,13),(18,6),(17,13),(22,18),(19,27),(15,22)],(115,73,184))
        draw_line(p,12,2,13,13,(242,228,255)); draw_line(p,13,13,10,20,(218,194,255)); put_pixel(p,20,11,(255,255,255))
    elif model_id==7805:
        # Path Changer: four-point compass rose.
        draw_circle(p,16,16,14,(10,20,41)); draw_circle(p,16,16,14,(102,208,207),True); draw_circle(p,16,16,10,(21,52,69),True)
        draw_polygon(p,[(16,4),(20,15),(16,16),(12,15)],(230,255,246),(255,255,255))
        draw_polygon(p,[(16,28),(12,17),(16,16),(20,17)],(17,121,139),(84,247,226))
        draw_polygon(p,[(4,16),(15,12),(16,16),(15,20)],(74,166,177),(203,255,245))
        draw_polygon(p,[(28,16),(17,20),(16,16),(17,12)],(37,79,114),(84,247,226)); put_pixel(p,16,16,(255,255,255))
    elif model_id==7890:
        # FX centerpiece: a compact gold hex crystal.
        draw_polygon(p,[(16,1),(24,7),(27,17),(16,31),(5,17),(8,7)],(39,25,6),(83,47,6),2)
        draw_polygon(p,[(16,3),(22,8),(24,17),(16,28),(8,17),(10,8)],(128,75,8),(255,190,49))
        draw_polygon(p,[(16,3),(16,16),(10,8)],(255,230,126)); draw_polygon(p,[(16,3),(22,8),(16,16)],(218,145,22))
        draw_polygon(p,[(10,8),(16,16),(8,17)],(255,190,40)); draw_polygon(p,[(16,16),(24,17),(16,28)],(101,54,6))
        draw_line(p,16,3,16,28,(255,245,174))
    return p


def fx_particle_texture(kind):
    """Original 32px pixel-art sprites for EvolutionFX; no third-party texture reuse."""
    p=blank_canvas()
    white=(255,255,245); cyan=(105,225,255); blue=(35,105,220)
    gold=(255,205,75); orange=(255,105,45); violet=(190,115,255)
    if kind=="spark":
        draw_polygon(p,[(16,1),(19,12),(29,16),(19,19),(16,31),(13,19),(3,16),(13,12)],cyan,white,1)
        draw_line(p,16,5,16,27,white,1)
    elif kind=="shard":
        draw_polygon(p,[(17,1),(25,10),(20,17),(22,25),(12,31),(10,20),(5,13)],blue,cyan,1)
        draw_polygon(p,[(17,3),(18,16),(10,20),(7,13)],white)
        draw_line(p,18,16,14,27,cyan,1)
    elif kind=="rune":
        draw_circle(p,16,16,13,violet,True)
        draw_circle(p,16,16,10,(22,12,42),True)
        draw_polygon(p,[(16,5),(22,15),(16,27),(10,15)],violet,white,1)
        draw_line(p,10,15,22,15,white,1)
    elif kind=="bliss":
        draw_polygon(p,[(16,0),(20,9),(29,5),(24,14),(31,19),(21,21),(18,31),(13,22),(4,27),(8,17),(1,12),(12,10)],gold,(255,250,220),1)
        draw_polygon(p,[(16,4),(19,13),(16,17),(12,12)],(255,255,235))
        draw_line(p,16,4,16,28,(255,255,245),1)
        draw_line(p,6,17,25,17,(255,238,170),1)
    elif kind=="rift":
        draw_polygon(p,[(13,0),(21,5),(17,12),(23,18),(18,31),(12,25),(14,18),(8,12)],(92,8,28),(255,74,105),2)
        draw_line(p,17,3,13,12,(255,205,215),2)
        draw_line(p,13,12,18,17,(255,90,125),2)
        draw_line(p,18,17,13,27,(255,220,225),2)
        draw_line(p,5,8,10,12,(255,74,105),1)
        draw_line(p,21,22,27,26,(255,74,105),1)
    elif kind=="bloom":
        draw_circle(p,16,16,13,(255,255,245),True)
        draw_circle(p,16,16,9,(255,116,135),True)
        draw_circle(p,16,16,5,(255,255,245),True)
        for angle in range(0,360,45):
            rad=math.radians(angle)
            draw_line(p,round(16+math.cos(rad)*9),round(16+math.sin(rad)*9),
                      round(16+math.cos(rad)*15),round(16+math.sin(rad)*15),(255,245,230),2)
    elif kind=="void":
        draw_circle(p,16,16,14,(155,95,255),True)
        draw_circle(p,16,16,11,(20,9,38),True)
        draw_circle(p,16,16,8,(111,55,190),True)
        draw_circle(p,16,16,5,(14,7,28),True)
        draw_line(p,20,5,26,10,(225,195,255),2)
        draw_line(p,11,23,6,27,(178,130,255),2)
    elif kind=="seal":
        draw_circle(p,16,16,14,(255,213,110),False)
        draw_circle(p,16,16,10,(255,246,205),False)
        draw_polygon(p,[(16,5),(20,14),(16,18),(12,14)],gold,(255,255,235),1)
        draw_line(p,16,18,16,27,(255,238,170),2)
        for angle in range(0,360,45):
            rad=math.radians(angle)
            draw_line(p,round(16+math.cos(rad)*11),round(16+math.sin(rad)*11),
                      round(16+math.cos(rad)*14),round(16+math.sin(rad)*14),(255,255,235),1)
    elif kind=="spire":
        draw_polygon(p,[(16,0),(22,9),(19,14),(24,21),(16,31),(8,21),(13,14),(10,9)],(12,54,95),(125,240,255),2)
        draw_polygon(p,[(16,3),(18,13),(16,25),(12,19)],(205,255,255))
        draw_line(p,16,2,16,29,(255,255,255),1)
        draw_line(p,6,22,11,18,(75,220,255),2)
        draw_line(p,21,18,26,22,(75,220,255),2)
    else: # flare
        draw_polygon(p,[(16,1),(20,11),(29,7),(23,17),(30,24),(19,22),(14,31),(12,20),(3,23),(9,15),(4,8),(14,11)],orange,gold,1)
        draw_circle(p,16,16,4,white,True)
    return p

def totem_texture():
    """A green-and-gold humanoid Totem texture, structurally unrelated to crystals."""
    p=blank_canvas(); gold=(215,163,44); light=(255,229,122); green=(49,139,91); mint=(156,230,154); dark=(43,31,17); deep=(15,66,55)
    draw_polygon(p,[(10,2),(21,2),(24,6),(24,14),(21,17),(10,17),(7,14),(7,6)],dark,dark,2)
    draw_polygon(p,[(11,3),(20,3),(22,6),(22,13),(20,15),(11,15),(9,13),(9,6)],gold,light)
    draw_polygon(p,[(10,6),(14,6),(14,9),(10,9)],deep); draw_polygon(p,[(18,6),(21,6),(21,9),(18,9)],deep)
    draw_polygon(p,[(14,12),(18,12),(17,14),(14,14)],green)
    draw_polygon(p,[(10,16),(6,18),(2,18),(1,22),(6,25),(10,23),(10,28),(13,31),(19,31),(22,28),(22,23),(26,25),(31,22),(30,18),(25,18),(21,16)],dark,dark)
    draw_polygon(p,[(11,17),(8,19),(4,19),(4,21),(8,23),(12,22),(12,28),(14,30),(18,30),(20,28),(20,22),(24,23),(28,21),(28,19),(24,19),(20,17)],gold,light)
    draw_polygon(p,[(12,18),(20,18),(20,25),(12,25)],green,mint); draw_polygon(p,[(14,20),(18,20),(18,27),(14,27)],deep)
    for x,y in [(4,10),(28,7),(3,27),(29,28)]: put_pixel(p,x,y,light)
    return p

def totem_particle_texture():
    """Eight-frame vertical Totem particle atlas (16x128), with a subtle pulsing rune."""
    width=16
    frames=[]
    for frame in range(8):
        p=[(0,0,0,0)]*(16*16)
        def put(x,y,c):
            if 0<=x<16 and 0<=y<16:
                p[y*16+x]=(*c,255)
        dark=(52,37,20); gold=(231,179,52); light=(255,238,151)
        green=(65+frame*2,157,93); deep=(20,81,65); shine=(255,249,202)
        # A compact idol: head, deep eyes, broad shoulders and a robe.
        for y in range(2,6):
            for x in range(5,11):
                put(x,y,dark if x in (5,10) or y==2 else gold)
        for x in (6,9): put(x,3,deep)
        put(7,3,light); put(8,3,light)
        for x in range(4,12): put(x,6,gold if x not in (4,11) else dark)
        for x in range(3,13): put(x,7,green if 5<=x<=10 else gold)
        for x in range(4,12): put(x,8,green if 5<=x<=10 else gold)
        for x in range(5,11): put(x,9,gold)
        for x in range(5,11): put(x,10,gold if x in (5,10) else green)
        for x in range(6,10): put(x,11,dark if x in (6,9) else deep)
        for x in range(6,10): put(x,12,gold)
        # Frame-to-frame light sweeps keep the sprite animated without changing its silhouette.
        shine_x=2+(frame*2)%12
        put(shine_x,6,shine)
        if frame%2==0:
            put(7,1,light); put(8,1,light)
            put(4,8,light); put(11,8,light)
        else:
            put(6,13,shine); put(9,13,shine)
        frames.extend(p)
    return frames

def path_icon_texture(kind, primary, accent):
    """Create crisp transparent 32px path sigils without external image libraries."""
    pixels=[(0,0,0,0)]*(32*32)
    def put(x,y,color):
        if 0<=x<32 and 0<=y<32:
            pixels[y*32+x]=(*color,255)
    def line(x0,y0,x1,y1,color,width=2):
        steps=max(abs(x1-x0),abs(y1-y0),1)*3
        for i in range(steps+1):
            t=i/steps; x=round(x0+(x1-x0)*t); y=round(y0+(y1-y0)*t)
            for ox in range(-(width//2),width//2+1):
                for oy in range(-(width//2),width//2+1): put(x+ox,y+oy,color)
    def rect(x0,y0,x1,y1,color):
        for y in range(y0,y1+1):
            for x in range(x0,x1+1): put(x,y,color)
    def circle(cx,cy,r,color,fill=True):
        for y in range(cy-r,cy+r+1):
            for x in range(cx-r,cx+r+1):
                d=(x-cx)*(x-cx)+(y-cy)*(y-cy)
                if d<=r*r and (fill or d>=(r-2)*(r-2)): put(x,y,color)
    def polygon(points,color):
        ys=[p[1] for p in points]; xs=[p[0] for p in points]
        for y in range(min(ys),max(ys)+1):
            for x in range(min(xs),max(xs)+1):
                inside=False; j=len(points)-1
                for i in range(len(points)):
                    xi,yi=points[i]; xj,yj=points[j]
                    if ((yi>y)!=(yj>y)) and x<(xj-xi)*(y-yi)/(yj-yi+1e-9)+xi: inside=not inside
                    j=i
                if inside: put(x,y,color)
    if kind=="hunt":
        # Crossed, abstract blades used only as a small in-game path sigil.
        line(6,25,24,7,primary,3); line(7,7,25,25,primary,3)
        line(4,22,10,28,accent,2); line(22,4,28,10,accent,2)
        line(4,10,10,4,accent,2); line(22,28,28,22,accent,2)
    elif kind=="alchemy":
        rect(12,4,19,8,accent); rect(13,8,18,12,primary)
        polygon([(9,13),(22,13),(26,25),(22,29),(9,29),(5,25)],primary)
        rect(8,20,23,24,accent); line(9,16,21,16,accent,2)
    elif kind=="enchantment":
        polygon([(5,5),(15,8),(15,27),(5,24)],primary)
        polygon([(16,8),(27,5),(27,24),(16,27)],accent)
        line(16,8,16,28,(235,230,255),2); line(8,12,13,14,accent,1); line(19,13,24,11,primary,1)
        circle(21,19,2,(255,255,255),True)
    elif kind=="prospector":
        line(6,23,24,7,primary,4); line(5,8,25,6,accent,4)
        line(7,9,7,15,accent,2); line(24,6,27,11,accent,2)
    elif kind=="ender":
        circle(16,16,12,primary,False); circle(16,16,7,accent,True)
        circle(16,16,3,(20,18,45),True); put(17,15,(255,255,255)); put(15,17,(255,255,255))
    elif kind=="trade":
        polygon([(16,3),(26,10),(22,25),(16,29),(10,25),(6,10)],primary)
        polygon([(16,5),(23,11),(19,22),(16,25),(13,22),(9,11)],accent)
        line(16,5,16,25,(245,255,235),1)
    else: # calamity: fractured dark crystal
        polygon([(16,3),(25,10),(22,24),(16,29),(8,23),(6,11)],primary)
        polygon([(16,6),(21,11),(19,22),(16,25),(11,21),(10,12)],accent)
        line(16,7,14,14,(25,12,30),2); line(14,14,19,17,(25,12,30),2); line(19,17,15,24,(25,12,30),2)
    return pixels

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--output', default='build/pack/EvolutionSMP-ResourcePack-26.3.zip')
    ap.add_argument('--version', choices=['26.3'], default='26.3')
    args=ap.parse_args()

    root=ROOT/'build'
    assets_root=root/'assets'
    if assets_root.exists():
        shutil.rmtree(assets_root)
    namespace='evolutionsmp'
    assets=root/'assets'/namespace
    textures=assets/'textures/item'
    models=assets/'models/item'
    item_defs=assets/'items'
    models.mkdir(parents=True,exist_ok=True)
    item_defs.mkdir(parents=True,exist_ok=True)

    # Every custom item lives in the plugin namespace. Vanilla item models,
    # definitions, and textures are never replaced by this pack.
    model_textures={
        'evolution_orb_7801': orb_texture(7801),
        'evolution_orb_7802': orb_texture(7802),
        'evolution_orb_7803': orb_texture(7803),
        'evolution_orb_7804': orb_texture(7804),
        'evolution_path_changer': orb_texture(7805),
        'evolution_fx_gem': orb_texture(7890),
        'fx_particle_spark': fx_particle_texture("spark"),
        'fx_particle_shard': fx_particle_texture("shard"),
        'fx_particle_rune': fx_particle_texture("rune"),
        'fx_particle_flare': fx_particle_texture("flare"),
        'fx_particle_bliss': fx_particle_texture("bliss"),
        'fx_particle_rift': fx_particle_texture("rift"),
        'fx_particle_bloom': fx_particle_texture("bloom"),
        'fx_particle_void': fx_particle_texture("void"),
        'fx_particle_seal': fx_particle_texture("seal"),
        'fx_particle_spire': fx_particle_texture("spire"),
    }
    for name,pixels in model_textures.items():
        png_rgba(textures/f'{name}.png',pixels)

    path_specs={
        'hunt':((205,35,55),(255,205,215)),
        'alchemy':((35,175,105),(180,255,200)),
        'enchantment':((125,65,220),(225,195,255)),
        'prospector':((205,135,35),(255,230,135)),
        'ender':((35,145,215),(175,235,255)),
        'trade':((30,165,125),(175,255,220)),
        'calamity':((65,20,80),(190,55,115)),
    }
    for name,(primary,accent) in path_specs.items():
        png_rgba(textures/f'evolution_path_{name}.png',path_icon_texture(name,primary,accent))

    def write_model(name, texture_name):
        model={
            'parent':'minecraft:item/generated',
            'textures':{'layer0':f'{namespace}:item/{texture_name}'}
        }
        (models/f'{name}.json').write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')

    for name in model_textures:
        write_model(name,name)
    for name in path_specs:
        model_name=f'evolution_path_{name}'
        write_model(model_name,model_name)

    # Paper 26.3 custom item model component IDs, independent of vanilla carriers.
    item_models={
        'orb_t2':'evolution_orb_7801',
        'orb_t3':'evolution_orb_7802',
        'orb_t4':'evolution_orb_7803',
        'evolution_shard':'evolution_orb_7804',
        'path_changer':'evolution_path_changer',
        'fx_gem':'evolution_fx_gem',
        'fx_spark':'fx_particle_spark',
        'fx_shard':'fx_particle_shard',
        'fx_rune':'fx_particle_rune',
        'fx_flare':'fx_particle_flare',
        'fx_bliss':'fx_particle_bliss',
        'fx_rift':'fx_particle_rift',
        'fx_bloom':'fx_particle_bloom',
        'fx_void':'fx_particle_void',
        'fx_seal':'fx_particle_seal',
        'fx_spire':'fx_particle_spire',
    }
    for name in path_specs:
        item_models[f'path_{name}']=f'evolution_path_{name}'

    for item_id,model_name in item_models.items():
        definition={'model':{'type':'minecraft:model','model':f'{namespace}:item/{model_name}'}}
        (item_defs/f'{item_id}.json').write_text(json.dumps(definition,indent=2)+'\n',encoding='utf-8')

    # A custom animated Totem particle is an effects-only override; no vanilla
    # Totem item texture, model, or item definition is touched.
    particle_dir=root/'assets/minecraft/textures/particle'
    particle_dir.mkdir(parents=True,exist_ok=True)
    png_rgba(particle_dir/'totem_of_undying.png',totem_particle_texture(),16,128)
    (particle_dir/'totem_of_undying.png.mcmeta').write_text(
        json.dumps({'animation':{'frametime':2,'interpolate':False,'frames':list(range(8))}},indent=2)+'\n',
        encoding='utf-8'
    )

    pack_format=[97,1]
    (root/'pack.mcmeta').write_text(json.dumps({
        'pack':{'min_format':pack_format,'max_format':pack_format,
                'description':'§5EvolutionSMP §8• §fRiftbound Relics (26.3)'}
    },indent=2)+'\n',encoding='utf-8')
    (root/'README.txt').write_text(
        'EvolutionSMP Resource Pack 1.0.0\nMinecraft Java 26.3\n'
        'Custom namespaced Evolution orbs, shards, path sigils, Path Changer, and FX gem.\n'
        'Only the Totem particle atlas is overridden in the minecraft namespace; vanilla item/block assets remain untouched.\n',
        encoding='utf-8'
    )

    out=Path(args.output)
    out.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        pack_files=[root/'pack.mcmeta',root/'README.txt'] + sorted(assets_root.rglob('*'))
        for f in pack_files:
            if f.is_file() and f.resolve()!=out.resolve(): z.write(f,f.relative_to(root).as_posix())

    with zipfile.ZipFile(out) as z:
        names=set(z.namelist())
        assert 'pack.mcmeta' in names
        meta=json.loads(z.read('pack.mcmeta').decode('utf-8'))['pack']
        assert meta.get('min_format')==pack_format and meta.get('max_format')==pack_format
        forbidden_prefixes=(
            'assets/minecraft/models/',
            'assets/minecraft/items/',
            'assets/minecraft/textures/item/',
            'assets/minecraft/textures/block/',
        )
        forbidden=[name for name in names if name.startswith(forbidden_prefixes)]
        assert not forbidden, f'Pack must not replace vanilla item/block assets: {forbidden[:5]}'
        assert 'assets/minecraft/textures/particle/totem_of_undying.png' in names
        assert 'assets/minecraft/textures/particle/totem_of_undying.png.mcmeta' in names
        assert 'assets/evolutionsmp/items/orb_t2.json' in names
        assert 'assets/evolutionsmp/items/evolution_shard.json' in names
        assert len([name for name in names if name.startswith('assets/evolutionsmp/items/') and name.endswith('.json')])==len(item_models)
        for model_path in [name for name in names if name.endswith('.json') or name.endswith('.mcmeta')]:
            parsed=json.loads(z.read(model_path).decode('utf-8'))
            for texture in parsed.get('textures',{}).values():
                if texture.startswith(namespace+':item/'):
                    texture_path=f'assets/{namespace}/textures/{texture.split(":",1)[1]}.png'
                    assert texture_path in names, f'Missing texture {texture_path}'
            if model_path.startswith(f'assets/{namespace}/items/'):
                target=parsed.get('model',{}).get('model','')
                assert target.startswith(namespace+':item/')
                target_path=f'assets/{namespace}/models/{target.split(":",1)[1]}.json'
                assert target_path in names, f'Missing model target {target}'
        for name in names:
            if name.endswith('.png'):
                assert z.read(name).startswith(b'\x89PNG\r\n\x1a\n'), f'Invalid PNG: {name}'
        # Verify all seven distinct path sigils are packaged.
        for name in path_specs:
            assert f'assets/{namespace}/textures/item/evolution_path_{name}.png' in names
            assert f'assets/{namespace}/items/path_{name}.json' in names
    print(f'Created {out} ({out.stat().st_size} bytes, {len(item_models)} custom item models).')

if __name__ == '__main__':
    main()
