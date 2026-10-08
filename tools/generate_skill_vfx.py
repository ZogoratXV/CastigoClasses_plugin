"""Author reproducible VFX defaults for the 48 shipped abilities, independent of Unity assets."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
P='castigoclasses:textures/vfx/'

def cue(shape,color,radius=1,duration=16,height=2,texture='rune_ring',rotation=90,rings=1,column='ribbon',opacity=.75):
    return {'enabled':True,'shape':shape,'duration-ticks':duration,'radius':radius,'points':24,
        'particles':{'enabled':False,'particle':'DUST','count':1,'spread':0,'color':color,'size':.45},
        'sound':{'enabled':False},'mesh':{'height':height,'opacity':opacity,'rotation':rotation,'scroll':.8,
        'fadeIn':.08,'fadeOut':.4,'rings':rings,'ringGap':.3,'columnRadius':.22,
        'ringTexture':P+texture+'.png','columnTexture':P+column+'.png','tint':color}}

def make(cls,sid,effect,radius):
    color={'mago_bianco':'FFE29A','mago_nero':'963DFF','guerriero_scudo':'48A9FF','guerriero_due_mani':'FF913D','arciere':'45E7A3','mago':'AC73FF'}[cls]
    seal='holy_seal' if cls=='mago_bianco' else 'dark_seal' if cls=='mago_nero' else 'nature_seal' if cls=='arciere' else 'rune_ring'
    phases={name:{'enabled':False} for name in ('cast','trail','impact','telegraph','hit')}
    phases['cast']=cue('MESH_RING',color,.55,10,texture=seal)
    phases['impact']=cue('MESH_BURST',color,.65,12,height=1.8,texture='flare')
    description='Lampo di impatto e sigillo di lancio.'
    def stage(name,shape,**kw):phases[name]=cue(shape,color,**kw);return phases[name]
    if effect in ('ALLY_HEAL','HEAL'):
        large='intercessione' in sid
        phases['impact']=cue('HEALING_BEAM','49F5AB' if not large else 'FFE29A',2.15 if large else 1.65,36,height=4 if large else 2.8,texture='holy_seal',rings=3 if large else 2,column='healing_column')
        phases['impact']['sound']={'enabled':True,'id':'castigoclasses:skill.orison','volume':.75,'pitch':.85 if large else 1}
        phases['impact']['mesh']['columnRadius']=.42
        if 'orison' in sid:phases['cast']={'enabled':False}
        description='Anelli e colonna di guarigione sul destinatario'+(', più ampia e alta per Grande intercessione.' if large else '.')
    elif effect=='HOT':
        stage('impact','HEALING_BEAM',radius=.65,duration=24,height=1.8,texture='holy_seal',column='healing_column')
        stage('telegraph','MESH_RING',radius=.7,duration=22,texture='holy_seal',rotation=35,rings=2)
        stage('hit','MESH_WAVE',radius=.65,duration=12,texture='holy_seal')
        description='Breve luce curativa e due sigilli orbitanti per la durata della rigenerazione.'
    elif effect in ('CLEANSE','REPULSE','FROST_NOVA'):
        phases['cast']={'enabled':False} if effect=='CLEANSE' else cue('MESH_WAVE',color,radius,18,height=.2,texture='wave',rings=2)
        stage('impact','MESH_WAVE',radius=2 if effect=='CLEANSE' else radius,duration=24,texture='holy_seal' if effect=='CLEANSE' else 'wave',rings=2)
        if effect=='FROST_NOVA':phases['impact']['mesh']['tint']='A4EDFF'
        description={'CLEANSE':'Sigillo luminoso che si espande sul destinatario purificato.','REPULSE':'Onda di luce che parte dal mago e si allarga fino al raggio della spinta.','FROST_NOVA':'Doppia onda ghiacciata azzurra attorno al mago.'}[effect]
    elif effect=='LINK':
        stage('impact','MESH_SHIELD',radius=.9,height=1.8,duration=16,column='shield_grid',opacity=.45)
        stage('telegraph','MESH_BEAM',radius=.25,duration=22,column='ribbon',rotation=0,opacity=.55)
        description='Barriera iniziale sul protetto e filo luminoso verso il protettore mentre il vincolo è efficace.'
    elif effect in ('SANCTUARY','RUIN','VORTEX'):
        shape='MESH_VORTEX' if effect=='VORTEX' else 'MESH_RING'
        stage('impact','MESH_WAVE',radius=radius,duration=24,texture=seal,rings=2)
        stage('telegraph',shape,radius=radius,duration=22,height=3 if effect=='VORTEX' else .3,texture=seal,rotation=-120 if effect=='VORTEX' else -25 if effect=='RUIN' else 20,rings=2,column='ribbon',opacity=.6)
        description={'SANCTUARY':'Cerchio sacro dorato che delimita la zona protetta per tutta la sua durata.','RUIN':'Sigillo oscuro a terra, controrotante, per tutta la durata della zona dannosa.','VORTEX':'Tre nastri scuri a spirale convergono al centro della zona.'}[effect]
    elif effect in ('BOLT','CURSED_BOLT','LIGHTNING','FIREBALL','DRAIN','METEOR','BLINK'):
        tint='FF822E' if effect in ('FIREBALL','METEOR') else '65CFFF' if effect=='LIGHTNING' else 'E83D79' if effect=='DRAIN' else color
        phases['trail']=cue('MESH_BEAM',tint,.45 if effect in ('FIREBALL','METEOR') else .2,8,column='lightning' if effect=='LIGHTNING' else 'ribbon')
        phases['impact']=cue('MESH_BURST',tint,min(radius,4) if effect in ('FIREBALL','METEOR') else .8,24 if effect=='METEOR' else 12,texture='flare')
        if effect=='METEOR':stage('telegraph','MESH_RING',radius=radius,duration=24,texture='dark_seal',rotation=-60)
        if effect=='CURSED_BOLT':stage('telegraph','MESH_SIGIL',radius=.4,duration=22,height=2.2,texture='dark_seal',rotation=-40)
        if effect=='CURSED_BOLT':stage('hit','MESH_BURST',radius=.3,duration=8,height=1,texture='flare')
        if effect=='BLINK':stage('impact','MESH_WAVE',radius=1.3,duration=16,texture='wave')
        description={'BOLT':'Raggio sottile colorato e lampo nel punto colpito.','CURSED_BOLT':'Scia viola, impatto oscuro e sigillo della maledizione sul nemico.','LIGHTNING':'Scarica verticale azzurra sopra il bersaglio e lampo elettrico.','FIREBALL':'Scia incandescente e impatto espansivo arancio.','DRAIN':'Filo rosso fra mago e vittima e impulso sul bersaglio.','METEOR':'Cerchio di avvertimento, scia verticale e grande esplosione luminosa.','BLINK':'Traccia fra partenza e arrivo e anello sulla destinazione.'}[effect]
    elif effect in ('VULNERABILITY','DOT','FEAR','HEAL_BLOCK','STUDY'):
        stage('impact','MESH_SIGIL',radius=.6,duration=18,height=2.1,texture=seal,rotation=-90)
        stage('telegraph','MESH_SIGIL',radius={'VULNERABILITY':.5,'DOT':.35,'FEAR':.7,'HEAL_BLOCK':.6,'STUDY':.45}[effect],duration=22,height=2.2,texture=seal,rotation={'VULNERABILITY':-35,'DOT':80,'FEAR':-130,'HEAL_BLOCK':0,'STUDY':30}[effect])
        if effect=='DOT':stage('hit','MESH_BURST',radius=.3,duration=8,height=1,texture='flare')
        description={'VULNERABILITY':'Marchio oscuro lento sopra il nemico vulnerabile.','DOT':'Piccolo sigillo pulsante di consunzione sul nemico.','FEAR':'Ampio sigillo oscuro in rapida rotazione sul bersaglio intimorito.','HEAL_BLOCK':'Sigillo di interdizione fermo sul nemico con cure ridotte.','STUDY':'Marchio verde di mira sopra il bersaglio studiato.'}[effect]
    elif effect in ('GUARD','BULWARK','WARD','RECOVER'):
        shape='MESH_RING' if effect=='RECOVER' else 'MESH_SHIELD'
        stage('cast',shape,radius=1.5 if effect=='BULWARK' else 1,height=2.3,duration=18,column='shield_grid',texture='holy_seal',opacity=.5)
        stage('telegraph',shape,radius=1.5 if effect=='BULWARK' else 1,height=2.3 if effect=='BULWARK' else 1.8,duration=22,column='shield_grid',texture='holy_seal',opacity=.32)
        stage('hit','MESH_BURST',radius=.65,duration=8,height=1.2,texture='flare')
        description='Barriera curva che segue la direzione del personaggio e termina con la protezione.' if effect!='RECOVER' else 'Aura azzurra alla base del personaggio durante la riscossa.'
    elif effect in ('DASH','CHARGE','HUNTER_STEP','DISENGAGE'):
        stage('cast','MESH_WAVE',radius=.85,duration=12,texture='wave')
        stage('trail','MESH_BEAM',radius=.6 if effect=='CHARGE' else .3,duration=6,column='ribbon')
        if effect=='DASH':stage('telegraph','MESH_SHIELD',radius=1,duration=22,height=1.8,column='shield_grid',opacity=.3)
        description='Scia breve dietro il movimento reale'+(' e impatto sul nemico raggiunto.' if effect=='CHARGE' else '.')
    elif effect in ('PRECISE_SHOT','HINDERING_SHOT','DOUBLE_SHOT','MASTER_SHOT','COVER_FIRE'):
        stage('cast','MESH_BURST',radius=.3,duration=6,height=1.4,texture='flare')
        stage('trail','MESH_BEAM',radius=.3 if effect=='MASTER_SHOT' else .12,duration=6,column='ribbon')
        stage('impact','MESH_BURST',radius=1 if effect=='MASTER_SHOT' else .35,duration=12,texture='flare')
        if effect in ('HINDERING_SHOT','COVER_FIRE'):stage('telegraph','MESH_RING',radius=.65,duration=22,texture='nature_seal',rotation=-60)
        description={'PRECISE_SHOT':'Scia verde sottile agganciata al percorso reale della freccia.','HINDERING_SHOT':'Scia della freccia e anello di intralcio sul nemico colpito.','DOUBLE_SHOT':'Due scie distinte, una per ciascuna freccia realmente scoccata.','MASTER_SHOT':'Scia più spessa e impatto concentrato del colpo del maestro.','COVER_FIRE':'Scie dei tiri di copertura e anelli sui nemici rallentati.'}[effect]
    else:
        area=effect in ('SWEEP','LOW_SWEEP')
        stage('cast','MESH_WAVE' if area else 'MESH_SLASH',radius=radius if area else 1.8 if effect=='LONG_THRUST' else 1.3,duration=18 if effect=='HEAVY_STRIKE' else 10,height=.4 if effect=='LOW_SWEEP' else 1.7,texture='wave',column='slash',rotation=-90 if effect=='COUNTER' else 90,rings=2 if area else 1)
        stage('impact','MESH_BURST',radius=.8 if effect=='GUARD_BREAK' else .4,duration=12,height=1.2,texture='flare')
        if effect=='SHIELD_BASH':stage('cast','MESH_SHIELD',radius=.8,duration=10,height=1.3,column='shield_grid',opacity=.6)
        if effect in ('MELEE','LONG_THRUST'):stage('cast','MESH_THRUST',radius=2.8 if effect=='LONG_THRUST' else 1.6,duration=10,height=1.7,column='ribbon')
        if effect in ('STOP_STRIKE','SHIELD_BASH','LOW_SWEEP'):stage('telegraph','MESH_RING',radius=.5,duration=22,texture='wave',rotation=-45)
        if effect=='GUARD_BREAK':stage('telegraph','MESH_SIGIL',radius=.45,duration=22,height=2,texture='dark_seal')
        description={'MELEE':'Affondo luminoso corto davanti al guerriero e lampo sulla vittima.','SHIELD_BASH':'Impatto ravvicinato e anello sul nemico stordito/rallentato.','COUNTER':'Arco inverso del contrattacco e scintilla sulla vittima.','HEAVY_STRIKE':'Arco ampio più lento e impatto caldo del fendente pesante.','LONG_THRUST':'Affondo luminoso allungato frontalmente e impatto puntuale.','STOP_STRIKE':'Taglio breve e anello che permane sul nemico rallentato.','SWEEP':'Onda circolare al raggio della skill e lampi su ogni nemico colpito.','LOW_SWEEP':'Onda bassa e anelli sui bersagli rallentati.','GUARD_BREAK':'Taglio, forte impatto e marchio sul nemico reso vulnerabile.','COMBO':'Un arco e un impatto per ciascun colpo della sequenza realmente eseguito.'}.get(effect,'Taglio e impatto sulla vittima.')
    if effect not in ('ALLY_HEAL','HEAL'):
        sound='minecraft:entity.arrow.hit' if cls=='arciere' else 'minecraft:entity.player.attack.sweep' if cls.startswith('guerriero') else 'minecraft:entity.evoker.cast_spell' if cls=='mago_nero' else 'minecraft:block.amethyst_block.chime'
        audible='cast' if effect in ('GUARD','BULWARK','RECOVER','REPULSE','SWEEP','LOW_SWEEP','DASH','HUNTER_STEP','DISENGAGE','CHARGE') else 'impact'
        if phases[audible].get('enabled'):phases[audible]['sound']={'enabled':True,'id':sound,'volume':.45,'pitch':1}
    # Personal sigils must surround a player, while area telegraphs retain the exact gameplay radius.
    area=effect in ('SANCTUARY','RUIN','VORTEX','METEOR','REPULSE','SWEEP','LOW_SWEEP','FROST_NOVA')
    for phase,c in phases.items():
        if not c.get('enabled'):continue
        shape=c['shape']
        if shape=='MESH_RING' and not (area and phase in ('telegraph','impact')):
            c['radius']=max(c['radius'],1.45 if phase=='cast' else 1.15)
            c['mesh']['rings']=max(c['mesh']['rings'],2)
            c['mesh']['ringGap']=.12
        if shape=='HEALING_BEAM':c['radius']=max(c['radius'],1.3)
        if shape=='MESH_BEAM':
            c['radius']=min(1,c['radius']*1.45)
            c['duration-ticks']=max(8,c['duration-ticks'])
        if shape=='MESH_BURST':
            c['radius']=max(.5,c['radius']*1.25)
            c['duration-ticks']=max(18,c['duration-ticks']) if phase=='impact' else c['duration-ticks']
            c['mesh']['fadeIn']=.015
            c['mesh']['fadeOut']=.5
    audio={'mago_bianco':'cleric.holy_impact','mago_nero':'necromancer.shoot','guerriero_scudo':'cleric.bash','guerriero_due_mani':'warrior.warrior_slash1','arciere':'archer.awakened_archer_first_hit','mago':'mage.fire_ball'}[cls]
    audio={'ALLY_HEAL':'cleric.orbs','HEAL':'cleric.orbs','HOT':'cleric.holy_summon','SANCTUARY':'cleric.holy_summon','FIREBALL':'mage.fire_explode','METEOR':'mage.meteor_explosion','LIGHTNING':'mage.thunder_strike','FROST_NOVA':'mage.ice_break','BLINK':'mage.thunder_teleport','LONG_THRUST':'warrior.warrior_pierce','HEAVY_STRIKE':'warrior.warrior_stomp','DASH':'warrior.warrior_airdash','HUNTER_STEP':'assassin.aa_dash','DISENGAGE':'assassin.aa_dash','VORTEX':'necromancer.summon'}.get(effect,audio)
    for phase,c in phases.items():
        if c.get('enabled') and c['sound'].get('enabled'):
            c['sound']={'enabled':True,'id':'castigoclasses_audio:'+audio,'volume':.55,'pitch':1}
    if cls=='arciere' and phases['cast'].get('enabled'):
        phases['cast']['sound']={'enabled':True,'id':'castigoclasses_audio:archer.awakened_archer_arrow_shoot','volume':.5,'pitch':1}
    description=description.replace('Raggio sottile colorato e lampo nel punto colpito.','Scia intrecciata con nucleo chiaro, esplosione colorata e frammenti sul bersaglio.')
    description+=' Modelli energetici articolati originali, gesto dell’arma e audio dedicato.'
    return phases,description

def main():
    presets={};rows=[]
    for file in sorted((ROOT/'src/main/resources/classes').glob('*.yml')):
        text=file.read_text(encoding='utf-8-sig');cls=file.stem
        blocks=re.split(r'^  ([a-z0-9_]+):\s*$',text.split('skills:',1)[1],flags=re.M)
        for i in range(1,len(blocks),2):
            sid,body=blocks[i:i+2]
            def field(key):
                found=re.search(r'^    '+key+r':\s*(.+)$',body,re.M)
                return found.group(1).strip().strip("'\"") if found else '4'
            name,effect=field('name'),field('effect');phases,description=make(cls,sid,effect,float(field('radius')))
            presets[sid]={'class':cls,'name':name,'effect':effect,'description':description,'presentation':phases}
            rows.append(f'| {cls} | {name} | {description} |')
    assert len(presets)==48,len(presets)
    (ROOT/'src/main/resources/skill-vfx.json').write_text(json.dumps(presets,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    (ROOT/'docs/VFX-48-ABILITA.md').write_text('# VFX delle 48 abilità — beta.11\n\nSigilli personali ampliati, scie intrecciate, impatti con volume e frammenti in stile Minecraft. I raggi delle aree di gioco restano autorevoli. Nessun prefab o shader Unity viene eseguito dalla mod.\n\n| Classe | Abilità | Effetto |\n|---|---|---|\n'+'\n'.join(rows)+'\n',encoding='utf8')
    print('Generated 48 skill presets and visual catalog')

if __name__=='__main__':main()
