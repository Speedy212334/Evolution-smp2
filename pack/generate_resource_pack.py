#!/usr/bin/env python3
"""Generate deterministic 26.3 EvolutionSMP orb textures and a resource-pack ZIP."""
import json, math, os, struct, zlib, zipfile, argparse
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
    ap=argparse.ArgumentParser(); ap.add_argument('--output', default=None); ap.add_argument('--version', choices=['1.21.11','26.3'], default='26.3'); args=ap.parse_args()
    root=ROOT/'build'; assets=root/'assets/minecraft'; textures=assets/'textures/item'; models=assets/'models/item'
    # Unique, original pixel-art orb textures.
    models.mkdir(parents=True,exist_ok=True)
    # Model IDs must match EvolutionItemManager.modelFor exactly:
    # Tier II=7801 on echo_shard, Tier III=7802 on amethyst_shard,
    # Tier IV=7803 on nether_star, and Echo Shard=7804 on echo_shard.
    specs={7801:'tier-ii prism',7802:'tier-iii star',7803:'tier-iv crown',7804:'echo shard'}
    for model,shape in specs.items(): png_rgba(textures/f'evolution_orb_{model}.png',orb_texture(model))
    def write_base_model(material):
        base={'parent':'minecraft:item/generated','textures':{'layer0':f'minecraft:item/{material}'}}
        (models/f'{material}.json').write_text(json.dumps(base,indent=2)+'\n',encoding='utf-8')
    write_base_model('echo_shard')
    write_base_model('amethyst_shard')
    write_base_model('nether_star')
    png_rgba(textures/'evolution_path_changer.png',orb_texture(7805))
    compass_base={'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/compass'}}
    (models/'compass.json').write_text(json.dumps(compass_base,indent=2)+'\n',encoding='utf-8')
    (models/'evolution_path_changer.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/evolution_path_changer'}},indent=2)+'\n',encoding='utf-8')
    for model in specs:
        (models/f'evolution_orb_{model}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'minecraft:item/evolution_orb_{model}'}},indent=2)+'\n',encoding='utf-8')
    # Seven distinct path sigils share PAPER as a safe display carrier; vanilla item models remain untouched.
    path_specs={
        7901:('hunt',(205,35,55),(255,205,215)),
        7902:('alchemy',(35,175,105),(180,255,200)),
        7903:('enchantment',(125,65,220),(225,195,255)),
        7904:('prospector',(205,135,35),(255,230,135)),
        7905:('ender',(35,145,215),(175,235,255)),
        7906:('trade',(30,165,125),(175,255,220)),
        7907:('calamity',(65,20,80),(190,55,115)),
    }
    paper_base={'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/paper'}}
    for model_id,(name,primary,accent) in path_specs.items():
        png_rgba(textures/f'evolution_path_{name}.png',path_icon_texture(name,primary,accent))
        model_name=f'evolution_path_{name}'
        (models/f'{model_name}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'minecraft:item/{model_name}'}},indent=2)+'\n',encoding='utf-8')
    (models/'paper.json').write_text(json.dumps(paper_base,indent=2)+'\n',encoding='utf-8')
    # Custom-model-data 7890 is reserved for the FX-only 3D orbit gem.
    # The vanilla gold ingot is only changed when the display item carries this model ID.
    png_rgba(textures/'evolution_fx_gem.png',orb_texture(7890))
    gold_base={'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/gold_ingot'}}
    (models/'gold_ingot.json').write_text(json.dumps(gold_base,indent=2)+'\n',encoding='utf-8')
    (models/'evolution_fx_gem.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/evolution_fx_gem'}},indent=2)+'\n',encoding='utf-8')

    # Dedicated green/gold Totem item and particle textures; neither reuses Echo Shard pixels.
    png_rgba(textures/'totem_of_undying.png',totem_texture())
    (models/'totem_of_undying.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/totem_of_undying'}},indent=2),encoding='utf-8')
    particle_dir=root/'assets/minecraft/textures/particle'; particle_dir.mkdir(parents=True,exist_ok=True)
    png_rgba(particle_dir/'totem_of_undying.png',totem_particle_texture(),16,128)
    (particle_dir/'totem_of_undying.png.mcmeta').write_text(json.dumps({'animation':{'frametime':2,'interpolate':False,'frames':list(range(8))}},indent=2)+'\n',encoding='utf-8')

    # Minecraft 26.3 item definitions select custom models from the structured custom_model_data floats list.
    item_defs=root/'assets/minecraft/items'
    item_defs.mkdir(parents=True,exist_ok=True)
    (item_defs/'totem_of_undying.json').write_text(json.dumps({'model':{'type':'minecraft:model','model':'minecraft:item/totem_of_undying'}},indent=2),encoding='utf-8')
    def model_ref(name):
        return {'type':'minecraft:model','model':f'minecraft:item/{name}'}
    def write_select_model(item_name, model_ids, fallback_name):
        definition={'model':{
            'type':'minecraft:range_dispatch',
            'property':'minecraft:custom_model_data',
            'index':0,
            'entries':[{'threshold':model_id,'model':model_ref(model_name)} for model_id,model_name in sorted(model_ids.items())],
            'fallback':model_ref(fallback_name)
        }}
        (item_defs/f'{item_name}.json').write_text(json.dumps(definition,indent=2)+'\n',encoding='utf-8')
    write_select_model('echo_shard',{7801:'evolution_orb_7801',7804:'evolution_orb_7804'},'echo_shard')
    write_select_model('amethyst_shard',{7802:'evolution_orb_7802'},'amethyst_shard')
    write_select_model('nether_star',{7803:'evolution_orb_7803'},'nether_star')
    write_select_model('compass',{7805:'evolution_path_changer'},'compass')
    write_select_model('paper',{7901:'evolution_path_hunt',7902:'evolution_path_alchemy',7903:'evolution_path_enchantment',7904:'evolution_path_prospector',7905:'evolution_path_ender',7906:'evolution_path_trade',7907:'evolution_path_calamity'},'paper')
    write_select_model('gold_ingot',{7890:'evolution_fx_gem'},'gold_ingot')

    # These custom icons are also exposed as direct item models. Core and FX bind the
    # items to these identifiers explicitly, avoiding fragile CMD-only dispatch on newer clients.
    direct_item_models = ['evolution_path_changer', *[f'evolution_path_{name}' for _, (name, _, _) in path_specs.items()]]
    for item_model_name in direct_item_models:
        (item_defs/f'{item_model_name}.json').write_text(
            json.dumps({'model': model_ref(item_model_name)}, indent=2) + '\n', encoding='utf-8')

    # Minecraft Java 26.3 uses resource-pack format 97.1.
    pack_format=[75,0] if args.version=='1.21.11' else [97,1]
    (root/'pack.mcmeta').write_text(json.dumps({'pack':{'min_format':pack_format,'max_format':pack_format,'description':f'§5EvolutionSMP §8• §fRiftbound Relics ({args.version})'}},indent=2)+'\n',encoding='utf-8')
    (root/'README.txt').write_text('EvolutionSMP Resource Pack 1.0.0\nMinecraft Java 26.3\nIncludes Tier II-IV orbs, Echo Shards, Path Changer, seven path sigils, and FX gem.\nThe Core plugin can deliver this pack automatically from a public HTTPS URL.\n',encoding='utf-8')
    output=args.output or ('dist/EvolutionSMP-ResourcePack-1.21.11.zip' if args.version=='1.21.11' else 'dist/EvolutionSMP-ResourcePack-26.3.zip')
    out=Path(output); out.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for f in sorted(root.rglob('*')):
            if f.is_file(): z.write(f,f.relative_to(root).as_posix())
    with zipfile.ZipFile(out) as z:
        names=set(z.namelist())
        assert 'pack.mcmeta' in names
        pack_meta=json.loads(z.read('pack.mcmeta').decode('utf-8'))['pack']
        expected_format=[75,0] if args.version=='1.21.11' else [97,1]
        assert pack_meta.get('min_format')==expected_format and pack_meta.get('max_format')==expected_format, f'Pack format mismatch for {args.version}'
        for base in ['echo_shard','amethyst_shard','nether_star','compass','paper','gold_ingot','totem_of_undying']:
            assert f'assets/minecraft/models/item/{base}.json' in names
            assert f'assets/minecraft/items/{base}.json' in names, f'Missing 26.3 item definition for {base}'
        assert 'assets/minecraft/textures/item/evolution_path_changer.png' in names
        assert 'assets/minecraft/textures/item/totem_of_undying.png' in names
        assert 'assets/minecraft/textures/particle/totem_of_undying.png' in names
        orb_pixels=[z.read(f'assets/minecraft/textures/item/evolution_orb_{i}.png') for i in (7801,7802,7803,7804)]
        assert len(set(orb_pixels))==4 and len(set(orb_pixels[:3]))==3, 'Tier II/III/IV orb shapes must all be distinct'
        assert z.read('assets/minecraft/textures/item/totem_of_undying.png') != orb_pixels[3]
        assert z.read('assets/minecraft/textures/particle/totem_of_undying.png') != orb_pixels[3]
        particle_png=z.read('assets/minecraft/textures/particle/totem_of_undying.png')
        assert struct.unpack('>II',particle_png[16:24])==(16,128), 'Totem particle must remain an 8-frame vertical sprite sheet'
        assert 'assets/minecraft/textures/particle/totem_of_undying.png.mcmeta' in names
        path_ids={'hunt':7901,'alchemy':7902,'enchantment':7903,'prospector':7904,'ender':7905,'trade':7906,'calamity':7907}
        paper_def=json.loads(z.read('assets/minecraft/items/paper.json').decode('utf-8'))['model']
        paper_entries={int(entry['threshold']):entry['model']['model'] for entry in paper_def.get('entries',[])}
        for path_name,model_id in path_ids.items():
            item_model_name=f'evolution_path_{path_name}'
            assert f'assets/minecraft/textures/item/{item_model_name}.png' in names
            assert f'assets/minecraft/models/item/{item_model_name}.json' in names
            assert paper_entries.get(model_id)==f'minecraft:item/{item_model_name}', f'Wrong CMD item-model mapping for {path_name}'
            direct=json.loads(z.read(f'assets/minecraft/items/{item_model_name}.json').decode('utf-8'))['model']
            assert direct=={'type':'minecraft:model','model':f'minecraft:item/{item_model_name}'}, f'Wrong direct item-model binding for {path_name}'
        changer_def=json.loads(z.read('assets/minecraft/items/evolution_path_changer.json').decode('utf-8'))['model']
        assert changer_def=={'type':'minecraft:model','model':'minecraft:item/evolution_path_changer'}, 'Wrong direct Path Changer item-model binding'
        assert 'assets/minecraft/textures/item/evolution_path_changer.png' in names
        parsed={}
        for model_path in [n for n in z.namelist() if n.endswith('.json') or n.endswith('.mcmeta')]:
            parsed[model_path]=json.loads(z.read(model_path).decode('utf-8'))
        for model_path, model in parsed.items():
            for texture in model.get('textures', {}).values():
                if texture.startswith('minecraft:item/evolution_'):
                    texture_path=f'assets/minecraft/textures/{texture.split(":",1)[1]}.png'
                    assert texture_path in names, f'Missing custom texture {texture_path} from {model_path}'
            if '/items/' in model_path:
                root_model=model.get('model',{})
                for entry in root_model.get('entries',[]):
                    target=entry.get('model',{}).get('model','')
                    if target:
                        namespace, resource=target.split(':',1)
                        assert f'assets/{namespace}/models/{resource}.json' in names, f'Missing model target {target} from {model_path}'
                fallback=root_model.get('fallback',{}).get('model','')
                if fallback:
                    namespace, resource=fallback.split(':',1)
                    assert f'assets/{namespace}/models/{resource}.json' in names, f'Missing fallback model {fallback} from {model_path}'
        for name in z.namelist():
            if name.endswith('.png'): assert z.read(name).startswith(b'\x89PNG\r\n\x1a\n')
    print(f'Created {out} ({out.stat().st_size} bytes, {len(specs)} orb variants).')

if __name__ == '__main__': main()
